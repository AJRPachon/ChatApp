package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ConversationBO
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(userId: String): Flow<List<ConversationBO>>
    suspend fun getOrCreateDirectConversation(currentUserId: String, otherUserId: String): ConversationBO
    suspend fun syncConversations(userId: String)
    suspend fun toggleMute(conversationId: String, muted: Boolean)
    suspend fun muteFor(conversationId: String, mutedUntil: Long)
    suspend fun clearChat(conversationId: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun archiveConversation(conversationId: String, archived: Boolean)
    fun observeArchivedConversations(userId: String): Flow<List<ConversationBO>>
    suspend fun setDisappearingMode(conversationId: String, seconds: Long)
}
