package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.MessageBO

/**
 * The local outbox for messages that haven't been confirmed sent yet, split out of
 * [MessageRepository] — a distinct concern (offline/retry queue management) from message CRUD,
 * used only by [ChatViewModel][com.ajrpachon.chatapp.ui.chat.ChatViewModel] (to enqueue) and
 * `MessageRetryWorker` (to drain the queue).
 */
interface PendingMessageRepository {
    @Suppress("LongParameterList")
    suspend fun savePendingMessage(
        id: String,
        conversationId: String,
        senderId: String,
        content: String,
        replyToId: String? = null,
        replyToContent: String? = null,
        replyToSenderName: String? = null,
    )
    suspend fun getPendingMessages(): List<MessageBO>
    suspend fun updateSendStatus(messageId: String, status: String)
}
