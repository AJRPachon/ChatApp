package com.ajrpachon.chatapp.data.remote.source

import com.ajrpachon.chatapp.data.remote.dto.TypingPresenceDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

/**
 * One Supabase Realtime presence channel per conversation (`typing-<conversationId>`), used to
 * share who is typing. Owns the channel lifecycle; deciding who counts as "typing" is the
 * repository's job.
 */
class TypingRemoteSource(private val supabase: SupabaseClient) {

    private val channels = ConcurrentHashMap<String, RealtimeChannel>()

    /** Subscribes to the conversation's channel; safe to call more than once (idempotent). */
    suspend fun subscribe(conversationId: String) {
        if (channels.containsKey(conversationId)) return
        val channel = supabase.channel(channelName(conversationId))
        channels[conversationId] = channel
        channel.subscribe()
    }

    /** Every participant's current presence payload, re-emitted whenever it changes. */
    fun observePresences(conversationId: String): Flow<List<TypingPresenceDTO>> {
        val channel = channels[conversationId]
            ?: supabase.channel(channelName(conversationId)).also { channels[conversationId] = it }
        return channel.presenceDataFlow<TypingPresenceDTO>()
    }

    /** Publishes this client's presence. Does nothing if the conversation was never subscribed. */
    suspend fun track(conversationId: String, presence: TypingPresenceDTO) {
        val channel = channels[conversationId] ?: return
        channel.track(presence)
    }

    suspend fun close(conversationId: String) {
        val channel = channels.remove(conversationId) ?: return
        channel.unsubscribe()
        supabase.realtime.removeChannel(channel)
    }

    private fun channelName(conversationId: String) = "typing-$conversationId"
}
