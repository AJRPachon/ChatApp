package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.GroupMemberDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class GroupRemoteSource(private val supabase: SupabaseClient) {

    suspend fun createGroup(
        convId: String,
        name: String,
        description: String?,
        createdBy: String,
        participantIds: List<String>,
    ) {
        supabase.postgrest["conversations"].insert(buildJsonObject {
            put("id", convId)
            put("name", name)
            put("is_group", true)
            put("created_by", createdBy)
            put("updated_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
            description?.let { put("description", it) }
        })
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString()
        for (uid in (listOf(createdBy) + participantIds).distinct()) {
            catchResult {
                supabase.postgrest["conversation_participants"].insert(buildJsonObject {
                    put("conversation_id", convId)
                    put("user_id", uid)
                    put("role", if (uid == createdBy) "admin" else "member")
                    put("joined_at", now)
                })
            }
        }
    }

    // null = network error; false = row absent (expelled/never joined); true = member
    suspend fun isCurrentUserMember(conversationId: String): Boolean? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return catchResult {
            supabase.postgrest["conversation_participants"]
                .select(Columns.list("user_id")) {
                    filter {
                        eq("conversation_id", conversationId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<JsonObject>()
                .isNotEmpty()
        }.onFailure { android.util.Log.e("GroupRemote", "isCurrentUserMember failed", it) }
         .getOrNull()
    }

    // null = network error; empty = RLS blocked (no rows returned)
    suspend fun getMembers(conversationId: String): List<GroupMemberDTO>? =
        catchResult {
            supabase.postgrest["conversation_participants"]
                .select(Columns.raw("conversation_id,user_id,role,joined_at,profiles!left(id,username,display_name,avatar_url)")) {
                    filter { eq("conversation_id", conversationId) }
                }
                .decodeList<GroupMemberDTO>()
        }.onFailure { android.util.Log.e("GroupRemote", "getMembers failed", it) }
         .getOrNull()

    // Emits an updated member list whenever INSERT or UPDATE fires on conversation_participants.
    // DELETE events are intentionally ignored — expulsion is detected by the polling loop in the
    // repository, which is more reliable than Realtime under RLS constraints.
    fun observeMembers(conversationId: String): Flow<List<GroupMemberDTO>> = channelFlow {
        val ch = supabase.channel("group-members-$conversationId")
        ch.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "conversation_participants"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }.onEach { action ->
            if (action is PostgresAction.Delete) return@onEach
            val members = getMembers(conversationId)
            if (members != null && members.isNotEmpty()) send(members)
        }.launchIn(this)

        ch.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { ch.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(ch) }
            }
        }
    }

    suspend fun addMember(conversationId: String, userId: String, canSeeHistory: Boolean) {
        val joinedAt = if (canSeeHistory) Instant.fromEpochMilliseconds(0).toString()
                       else Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString()
        supabase.postgrest["conversation_participants"].insert(buildJsonObject {
            put("conversation_id", conversationId)
            put("user_id", userId)
            put("role", "member")
            put("joined_at", joinedAt)
        })
    }

    suspend fun removeMember(conversationId: String, userId: String) {
        supabase.postgrest["conversation_participants"].delete {
            filter {
                eq("conversation_id", conversationId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun updateGroup(conversationId: String, name: String?, description: String?, avatarUrl: String?) {
        supabase.postgrest["conversations"].update(buildJsonObject {
            name?.let { put("name", it) }
            description?.let { put("description", it) }
            avatarUrl?.let { put("avatar_url", it) }
            put("updated_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
        }) { filter { eq("id", conversationId) } }
    }

    suspend fun updateMemberRole(conversationId: String, userId: String, role: String) {
        supabase.postgrest["conversation_participants"]
            .update(buildJsonObject { put("role", role) }) {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userId)
                }
            }
    }
}
