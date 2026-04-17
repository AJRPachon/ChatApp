package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(conversationId: String): Flow<List<MessageBO>>
    suspend fun sendMessage(conversationId: String, senderId: String, content: String): MessageBO
    suspend fun markAsRead(conversationId: String, userId: String)
    suspend fun syncMessages(conversationId: String)
}
