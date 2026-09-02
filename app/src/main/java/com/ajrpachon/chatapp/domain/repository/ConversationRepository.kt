package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ConversationBO
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun observeConversations(userId: String): Flow<List<ConversationBO>>

    /**
     * One-shot, local-only snapshot of [userId]'s conversations — reads Room directly
     * without opening the Realtime channels [observeConversations] does. Use this for
     * callers that just need a current list (e.g. resolving contact ids) and would
     * otherwise collide with an already-subscribed [observeConversations] channel
     * (`IllegalStateException: You cannot call postgresChangeFlow after joining the
     * channel`).
     */
    suspend fun getLocalConversations(userId: String): List<ConversationBO>
    suspend fun getOrCreateDirectConversation(currentUserId: String, otherUserId: String): ConversationBO
    suspend fun syncConversations(userId: String)
    suspend fun toggleMute(conversationId: String, muted: Boolean)
    suspend fun muteFor(conversationId: String, mutedUntil: Long)
    suspend fun clearChat(conversationId: String)
    suspend fun deleteConversation(conversationId: String)
    suspend fun archiveConversation(conversationId: String, archived: Boolean)
    fun observeArchivedConversations(userId: String): Flow<List<ConversationBO>>
    suspend fun setDisappearingMode(conversationId: String, seconds: Long)
    suspend fun getById(conversationId: String): ConversationBO?
    fun observeById(conversationId: String): Flow<ConversationBO?>
    suspend fun resetUnreadCount(conversationId: String)
}
