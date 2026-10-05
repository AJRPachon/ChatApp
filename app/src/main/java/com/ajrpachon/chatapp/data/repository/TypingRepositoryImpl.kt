package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.dto.TypingPresenceDTO
import com.ajrpachon.chatapp.data.remote.source.TypingRemoteSource
import com.ajrpachon.chatapp.domain.repository.TypingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TypingRepositoryImpl(
    private val remoteSource: TypingRemoteSource,
) : TypingRepository {

    /** Must be called once to subscribe; safe to call multiple times (idempotent). */
    override suspend fun subscribeChannel(conversationId: String) {
        remoteSource.subscribe(conversationId)
    }

    override fun observeTypingNames(conversationId: String, currentUserId: String): Flow<List<String>> =
        remoteSource.observePresences(conversationId)
            .map { presences ->
                presences
                    .filter { p -> p.isTyping && p.userId != currentUserId }
                    .map { p -> p.userName }
                    .distinct()
            }

    override suspend fun sendTypingState(conversationId: String, userId: String, userName: String, isTyping: Boolean) {
        remoteSource.track(conversationId, TypingPresenceDTO(isTyping = isTyping, userId = userId, userName = userName))
    }

    override suspend fun close(conversationId: String) {
        remoteSource.close(conversationId)
    }
}
