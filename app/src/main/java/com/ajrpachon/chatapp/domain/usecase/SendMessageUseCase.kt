package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.MessageLimits
import com.ajrpachon.chatapp.domain.repository.MessageRepository

class SendMessageUseCase(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(
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
        fileUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        fileMimeType: String? = null,
        videoUrl: String? = null,
        // E2EE: pass the other user's ID for 1:1 conversations (null for group chats)
        otherUserId: String? = null,
    ): Result<MessageBO> = catchResult {
        require(
            content.isNotBlank() || imageUrl != null || audioUrl != null ||
                    callType != null || gifUrl != null || stickerUrl != null ||
                    fileUrl != null || videoUrl != null
        ) { "Message cannot be blank" }
        require(content.length <= MessageLimits.MAX_CONTENT_LENGTH) {
            "Message exceeds ${MessageLimits.MAX_CONTENT_LENGTH} characters"
        }
        messageRepository.sendMessage(
            conversationId, senderId, content.trim(),
            imageUrl, audioUrl,
            replyToId, replyToContent, replyToSenderName,
            callType, callStatus, callDuration,
            gifUrl, stickerUrl,
            fileUrl, fileName, fileSize, fileMimeType,
            videoUrl = videoUrl,
            otherUserId = otherUserId,
        )
    }
}
