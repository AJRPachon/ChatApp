package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.dto.ReactionRemoteDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

private val lenientJson = Json { ignoreUnknownKeys = true }

class MessageRemoteSource(private val supabase: SupabaseClient) {

    suspend fun getMessages(conversationId: String, since: Long = 0L): List<MessageDTO> =
        supabase.postgrest["messages"]
            .select {
                filter {
                    eq("conversation_id", conversationId)
                    if (since > 0L) gte("created_at", kotlinx.datetime.Instant.fromEpochMilliseconds(since).toString())
                }
            }
            .decodeList<MessageDTO>()

    suspend fun getLastMessage(conversationId: String, since: Long = 0L): MessageDTO? =
        runCatching {
            supabase.postgrest["messages"]
                .select {
                    filter {
                        eq("conversation_id", conversationId)
                        if (since > 0L) gte("created_at", kotlinx.datetime.Instant.fromEpochMilliseconds(since).toString())
                    }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<MessageDTO>()
        }.getOrNull()

    suspend fun sendMessage(dto: MessageDTO) {
        supabase.postgrest["messages"].insert(dto)
    }

    suspend fun deleteMessage(messageId: String) {
        supabase.postgrest["messages"]
            .update({ set("is_deleted", true) }) {
                filter { eq("id", messageId) }
            }
    }

    suspend fun editMessage(messageId: String, newContent: String) {
        supabase.postgrest["messages"]
            .update({
                set("content", newContent)
                set("is_edited", true)
                set("edited_at", Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
            }) {
                filter { eq("id", messageId) }
            }
    }

    suspend fun markAsRead(conversationId: String, userId: String) {
        supabase.postgrest["messages"]
            .update({ set("is_read", true) }) {
                filter {
                    eq("conversation_id", conversationId)
                    neq("sender_id", userId)
                    eq("is_read", false)
                }
            }
    }

    fun observeMessageUpdates(conversationId: String): Flow<MessageDTO> = channelFlow {
        val channel = supabase.channel("messages:updates:$conversationId")
        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "messages"
        }.onEach { action ->
            runCatching {
                val dto = lenientJson.decodeFromString<MessageDTO>(action.record.toString())
                if (dto.conversationId == conversationId) trySend(dto)
            }
        }.launchIn(this)
        channel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { channel.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(channel) }
            }
        }
    }

    suspend fun getReactionsForMessages(messageIds: List<String>): List<ReactionRemoteDTO> {
        if (messageIds.isEmpty()) return emptyList()
        return runCatching {
            supabase.postgrest["message_reactions"]
                .select {
                    filter { isIn("message_id", messageIds) }
                }
                .decodeList<ReactionRemoteDTO>()
        }.getOrDefault(emptyList())
    }

    // Cleanup via withContext(NonCancellable) ensures unsubscribe + removeChannel complete
    // synchronously before the flow terminates, preventing reuse of a stale joined channel.
    fun observeNewMessages(conversationId: String): Flow<MessageDTO> = channelFlow {
        val channel = supabase.channel("messages:$conversationId")
        channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.onEach { action ->
            runCatching {
                val dto = lenientJson.decodeFromString<MessageDTO>(action.record.toString())
                if (dto.conversationId == conversationId) trySend(dto)
            }
        }.launchIn(this)
        channel.subscribe()
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                runCatching { channel.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(channel) }
            }
        }
    }
}
