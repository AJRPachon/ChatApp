package com.ajrpachon.chatapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface TypingRepository {
    fun observeTypingNames(conversationId: String, currentUserId: String): Flow<List<String>>
    suspend fun sendTypingState(conversationId: String, userId: String, userName: String, isTyping: Boolean)
    suspend fun close(conversationId: String)
    suspend fun subscribeChannel(conversationId: String)
}
