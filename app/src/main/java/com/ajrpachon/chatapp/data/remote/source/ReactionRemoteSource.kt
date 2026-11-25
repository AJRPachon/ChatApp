package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.ReactionChangeDTO
import com.ajrpachon.chatapp.data.remote.dto.ReactionRemoteDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
import kotlinx.serialization.json.Json
import java.util.UUID

private val reactionJson = Json { ignoreUnknownKeys = true }

class ReactionRemoteSource(private val supabase: SupabaseClient) {

    suspend fun deleteReaction(messageId: String, userId: String, emoji: String) {
        supabase.postgrest["message_reactions"].delete {
            filter {
                eq("message_id", messageId)
                eq("user_id", userId)
                eq("emoji", emoji)
            }
        }
    }

    suspend fun insertReaction(messageId: String, userId: String, emoji: String) {
        supabase.postgrest["message_reactions"].insert(
            ReactionRemoteDTO(messageId, userId, emoji)
        )
    }

    /**
     * Reactions added or removed in any conversation the user takes part in (RLS limits the
     * inserts; deletes carry only the primary key). The table has no conversation id, so callers
     * apply every change: Room only shows reactions whose message belongs to the open chat.
     */
    fun observeReactionChanges(): Flow<ReactionChangeDTO> = channelFlow {
        // Random suffix: each subscriber gets its own channel instead of an already-joined one.
        val channel = supabase.channel("message_reactions:${UUID.randomUUID()}")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "message_reactions"
        }.onEach { action ->
            val (record, removed) = when (action) {
                is PostgresAction.Insert -> action.record to false
                is PostgresAction.Delete -> action.oldRecord to true
                else -> return@onEach
            }
            runCatching { reactionJson.decodeFromString<ReactionRemoteDTO>(record.toString()) }
                .onSuccess { trySend(ReactionChangeDTO(it, removed)) }
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
