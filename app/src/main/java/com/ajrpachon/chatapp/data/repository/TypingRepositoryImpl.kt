package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.domain.repository.TypingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

class TypingRepositoryImpl(
    private val supabase: SupabaseClient,
) : TypingRepository {

    @Serializable
    private data class TypingPresence(
        val isTyping: Boolean,
        val userId: String,
        val userName: String,
    )

    private val channels = ConcurrentHashMap<String, RealtimeChannel>()

    /** Must be called once to subscribe; safe to call multiple times (idempotent). */
    suspend fun subscribeChannel(conversationId: String) {
        if (channels.containsKey(conversationId)) return
        val ch = supabase.channel("typing-$conversationId")
        channels[conversationId] = ch
        ch.subscribe()
    }

    override fun observeTypingNames(conversationId: String, currentUserId: String): Flow<List<String>> {
        val ch = channels[conversationId] ?: supabase.channel("typing-$conversationId").also { channels[conversationId] = it }
        return ch.presenceDataFlow<TypingPresence>()
            .map { presences ->
                presences
                    .filter { p -> p.isTyping && p.userId != currentUserId }
                    .map { p -> p.userName }
                    .distinct()
            }
    }

    override suspend fun sendTypingState(conversationId: String, userId: String, userName: String, isTyping: Boolean) {
        val ch = channels[conversationId] ?: return
        ch.track(TypingPresence(isTyping = isTyping, userId = userId, userName = userName))
    }

    override suspend fun close(conversationId: String) {
        val ch = channels.remove(conversationId) ?: return
        ch.unsubscribe()
        supabase.realtime.removeChannel(ch)
    }
}
