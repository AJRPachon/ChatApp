package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.ConversationParticipantWithConvDTO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val lenientJson = Json { ignoreUnknownKeys = true }

@Serializable
internal data class ParticipantUserIdDTO(@SerialName("user_id") val userId: String)

// Every Realtime channel below gets a per-subscription suffix: the same user's conversation list can
// be collected more than once concurrently (e.g. ConversationListViewModel's long-lived collection
// plus ChatForwardDelegate.showForwardDialog's one-shot `.first()` call while the list screen is
// still alive in the back stack). supabase-kt looks channels up by topic name, so two collectors
// sharing a fixed topic get the SAME RealtimeChannel instance, and the second collector's
// postgresChangeFlow() call then throws "You cannot call postgresChangeFlow after joining the
// channel". That IllegalStateException was swallowed upstream before it reached the UI, so
// e.g. showForwardDialog silently failed to ever show.
private fun uniqueTopic(prefix: String, userId: String) = "$prefix:$userId-${System.nanoTime()}"

class ConversationRemoteSource(private val supabase: SupabaseClient) {

    /**
     * The signed-in user's id. Reading the stored session also makes sure the user JWT is loaded,
     * which the Realtime subscriptions below need to be authenticated.
     */
    fun currentUserId(): String? = supabase.auth.currentSessionOrNull()?.user?.id

    suspend fun fetchParticipantsWithConversations(userId: String): List<ConversationParticipantWithConvDTO> =
        supabase.postgrest["conversation_participants"]
            .select(
                Columns.raw(
                    "conversation_id, joined_at, conversations(id,name,is_group,created_by,updated_at,avatar_url,description)"
                )
            ) {
                filter { eq("user_id", userId) }
            }
            .decodeList()

    suspend fun fetchOtherParticipantId(conversationId: String, excludeUserId: String): String? =
        supabase.postgrest["conversation_participants"]
            .select(Columns.list("user_id")) {
                filter {
                    eq("conversation_id", conversationId)
                    neq("user_id", excludeUserId)
                }
            }
            .decodeList<ParticipantUserIdDTO>()
            .firstOrNull()
            ?.userId

    suspend fun fetchUserProfile(userId: String): UserDTO? =
        supabase.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()

    suspend fun getOrCreateDirectConversation(userA: String, userB: String): String {
        val result = supabase.postgrest.rpc(
            "get_or_create_direct_conversation",
            buildJsonObject {
                put("user_a", userA)
                put("user_b", userB)
            },
        )
        return Json.decodeFromString<String>(result.data)
    }

    fun observeParticipantInserts(userId: String): Flow<JsonObject> = channelFlow {
        val ch = supabase.channel(uniqueTopic("participants", userId))
        val flow = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "conversation_participants"
        }
        ch.subscribe()
        try {
            flow.collect { action -> send(action.record) }
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { ch.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(ch) }
            }
        }
    }

    fun observeNewMessageInserts(userId: String): Flow<MessageDTO> = channelFlow {
        val ch = supabase.channel(uniqueTopic("messages:list", userId))
        val flow = ch.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }
        ch.subscribe()
        try {
            flow.collect { action ->
                runCatching {
                    lenientJson.decodeFromString<MessageDTO>(action.record.toString())
                }.getOrNull()?.let { send(it) }
            }
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { ch.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(ch) }
            }
        }
    }

    fun observeConversationUpdates(userId: String): Flow<JsonObject> = channelFlow {
        val ch = supabase.channel(uniqueTopic("conversations:updates", userId))
        val flow = ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "conversations"
        }
        ch.subscribe()
        try {
            flow.collect { action -> send(action.record) }
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { ch.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(ch) }
            }
        }
    }

    fun observeProfileUpdates(userId: String): Flow<JsonObject> = channelFlow {
        val ch = supabase.channel(uniqueTopic("profiles:updates", userId))
        val flow = ch.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "profiles"
        }
        ch.subscribe()
        try {
            flow.collect { action -> send(action.record) }
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { ch.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(ch) }
            }
        }
    }
}
