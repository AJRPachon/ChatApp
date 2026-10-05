package com.ajrpachon.chatapp.domain.model

/**
 * A message about to be sent: the single parameter of
 * [SendMessageUseCase][com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase] and
 * [MessageRepository.sendMessage][com.ajrpachon.chatapp.domain.repository.MessageRepository.sendMessage].
 *
 * It bundles what used to be a 22-argument function. A message normally carries one kind of
 * payload (text, one attachment, or a call summary) plus optional reply context, so most fields
 * stay null; build it with named arguments and set only what the message needs.
 */
data class OutgoingMessage(
    val conversationId: String,
    val senderId: String,
    val content: String,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val audioDurationMs: Long? = null,
    val audioAmplitudes: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val callType: String? = null,
    val callStatus: String? = null,
    val callDuration: Int? = null,
    val gifUrl: String? = null,
    val stickerUrl: String? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileMimeType: String? = null,
    val videoUrl: String? = null,
    // E2EE: the other user's ID for 1:1 conversations (null for group chats)
    val otherUserId: String? = null,
    val statusReply: StatusReplyContext? = null,
) {
    /** True when the message carries something other than text: media, a file, a sticker or a call summary. */
    val hasNonTextPayload: Boolean
        get() = imageUrl != null || audioUrl != null || callType != null || gifUrl != null ||
            stickerUrl != null || fileUrl != null || videoUrl != null
}
