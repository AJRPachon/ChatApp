package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.ConversationParticipantWithConvDTO
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
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

class ConversationRemoteSource(private val supabase: SupabaseClient) {

    fun ensureSession(): UserSession? = supabase.auth.currentSessionOrNull()

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
        val ch = supabase.channel("participants:$userId")
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
        val ch = supabase.channel("messages:list:$userId")
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
        val ch = supabase.channel("conversations:updates:$userId-${System.nanoTime()}")
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
        val ch = supabase.channel("profiles:updates:$userId-${System.nanoTime()}")
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
