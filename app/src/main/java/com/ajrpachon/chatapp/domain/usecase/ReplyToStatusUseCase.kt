package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.domain.model.StatusReplyContext

data class StatusReplyResult(val conversationId: String, val otherUserName: String)

/**
 * WhatsApp-style "reply to a status": sends the reply as a normal message in the 1:1 conversation
 * with the status's owner, carrying a [StatusReplyContext] snapshot so the chat bubble can show
 * the quoted status without depending on it still existing 24h later.
 */
class ReplyToStatusUseCase(
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
) {
    suspend operator fun invoke(currentUserId: String, status: StatusBO, text: String): Result<StatusReplyResult> =
        runCatching {
            val conversation = getOrCreateConversationUseCase(currentUserId, status.userId)
            sendMessageUseCase(
                conversationId = conversation.id,
                senderId = currentUserId,
                content = text,
                otherUserId = status.userId,
                statusReply = StatusReplyContext(
                    statusId = status.id,
                    statusOwnerId = status.userId,
                    statusText = status.text,
                    statusImageUrl = status.imageUrl,
                    statusVideoUrl = status.videoUrl,
                    statusBackgroundColor = status.backgroundColor,
                    statusExpiresAt = status.expiresAt.toEpochMilliseconds(),
                ),
            ).getOrThrow()
            StatusReplyResult(conversation.id, status.userName)
        }
}
