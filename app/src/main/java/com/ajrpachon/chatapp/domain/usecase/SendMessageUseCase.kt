package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.MessageLimits
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import com.ajrpachon.chatapp.utils.catchResult

class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        conversationId: String,
        senderId: String,
        content: String,
        imageUrl: String? = null,
        audioUrl: String? = null,
        audioDurationMs: Long? = null,
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
        val message = messageRepository.sendMessage(
            conversationId, senderId, content.trim(),
            imageUrl, audioUrl, audioDurationMs,
            replyToId, replyToContent, replyToSenderName,
            callType, callStatus, callDuration,
            gifUrl, stickerUrl,
            fileUrl, fileName, fileSize, fileMimeType,
            videoUrl = videoUrl,
            otherUserId = otherUserId,
        )
        // Call-summary messages (callType != null) are not user-authored content — call
        // analytics are logged symmetrically from CallViewModel itself instead, covering both
        // call directions, not just this outgoing-only summary message.
        if (callType == null) {
            logMessageSentAnalytics(imageUrl, videoUrl, audioUrl, gifUrl, stickerUrl, fileUrl)
        }
        message
    }

    @Suppress("LongParameterList")
    private fun logMessageSentAnalytics(
        imageUrl: String?,
        videoUrl: String?,
        audioUrl: String?,
        gifUrl: String?,
        stickerUrl: String?,
        fileUrl: String?,
    ) {
        val messageType = when {
            imageUrl != null -> AnalyticsEvents.TYPE_IMAGE
            videoUrl != null -> AnalyticsEvents.TYPE_VIDEO
            audioUrl != null -> AnalyticsEvents.TYPE_AUDIO
            gifUrl != null -> AnalyticsEvents.TYPE_GIF
            stickerUrl != null -> AnalyticsEvents.TYPE_STICKER
            fileUrl != null -> AnalyticsEvents.TYPE_FILE
            else -> AnalyticsEvents.TYPE_TEXT
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.MESSAGE_SENT,
            mapOf(AnalyticsEvents.PARAM_MESSAGE_TYPE to messageType),
        )
    }
}
