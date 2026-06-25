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
    ): Result<MessageBO> = catchResult {
        require(
            content.isNotBlank() || imageUrl != null || audioUrl != null ||
                    callType != null || gifUrl != null || stickerUrl != null
        ) { "Message cannot be blank" }
        if (content.length > MessageLimits.MAX_CONTENT_LENGTH)
            return@catchResult Result.failure(IllegalArgumentException("Message exceeds ${MessageLimits.MAX_CONTENT_LENGTH} characters"))
        messageRepository.sendMessage(
            conversationId, senderId, content.trim(),
            imageUrl, audioUrl,
            replyToId, replyToContent, replyToSenderName,
            callType, callStatus, callDuration,
            gifUrl, stickerUrl,
        )
    }
}
