package com.ajrpachon.chatapp.domain.repository

import androidx.paging.PagingData
import com.ajrpachon.chatapp.domain.model.MessageBO
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(conversationId: String, currentUserId: String, historyVisibleFrom: Long = 0L): Flow<List<MessageBO>>
    fun syncRemote(conversationId: String, historyVisibleFrom: Long = 0L): Flow<Unit>
    fun getMessagesPaged(conversationId: String, currentUserId: String, historyVisibleFrom: Long = 0L): Flow<PagingData<MessageBO>>
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        content: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        replyToId: String? = null,
        replyToContent: String? = null,
        replyToSenderName: String? = null,
        callType: String? = null,
        callStatus: String? = null,
        callDuration: Int? = null,
        gifUrl: String? = null,
        stickerUrl: String? = null,
        // E2EE: pass the other user's ID for 1:1 conversations (null = skip encryption)
        otherUserId: String? = null,
    ): MessageBO
    suspend fun uploadImage(conversationId: String, bytes: ByteArray, mimeType: String): String
    suspend fun uploadAudio(conversationId: String, bytes: ByteArray): String
    suspend fun markAsRead(conversationId: String, userId: String)
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun syncMessages(conversationId: String, since: Long = 0L)
    suspend fun clearMessages(conversationId: String)
}
