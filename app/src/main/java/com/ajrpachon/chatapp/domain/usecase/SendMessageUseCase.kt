package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.MessageLimits
import com.ajrpachon.chatapp.domain.model.OutgoingMessage
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import com.ajrpachon.chatapp.utils.catchResult

class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend operator fun invoke(message: OutgoingMessage): Result<MessageBO> = catchResult {
        require(message.content.isNotBlank() || message.hasNonTextPayload) { "Message cannot be blank" }
        require(message.content.length <= MessageLimits.MAX_CONTENT_LENGTH) {
            "Message exceeds ${MessageLimits.MAX_CONTENT_LENGTH} characters"
        }
        val sent = messageRepository.sendMessage(message.copy(content = message.content.trim()))
        // Call-summary messages (callType != null) are not user-authored content — call
        // analytics are logged symmetrically from CallViewModel itself instead, covering both
        // call directions, not just this outgoing-only summary message.
        if (message.callType == null) {
            logMessageSentAnalytics(message)
        }
        sent
    }

    private fun logMessageSentAnalytics(message: OutgoingMessage) {
        val messageType = when {
            message.imageUrl != null -> AnalyticsEvents.TYPE_IMAGE
            message.videoUrl != null -> AnalyticsEvents.TYPE_VIDEO
            message.audioUrl != null -> AnalyticsEvents.TYPE_AUDIO
            message.gifUrl != null -> AnalyticsEvents.TYPE_GIF
            message.stickerUrl != null -> AnalyticsEvents.TYPE_STICKER
            message.fileUrl != null -> AnalyticsEvents.TYPE_FILE
            else -> AnalyticsEvents.TYPE_TEXT
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.MESSAGE_SENT,
            mapOf(AnalyticsEvents.PARAM_MESSAGE_TYPE to messageType),
        )
    }
}
