package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ConversationBO
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(userId: String): Flow<List<ConversationBO>>
    suspend fun getOrCreateDirectConversation(currentUserId: String, otherUserId: String): ConversationBO
    suspend fun syncConversations(userId: String)
}
