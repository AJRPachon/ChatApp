package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class MessageBO(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val isRead: Boolean,
    val isFromMe: Boolean,
    val createdAt: Instant,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val callType: String? = null,
    val callStatus: String? = null,
    val callDuration: Int? = null,
    val gifUrl: String? = null,
    val stickerUrl: String? = null,
) {
    val isCallMessage: Boolean get() = callType != null

    fun replySnippet(): String = when {
        callType != null -> if (callType == "video") "Videollamada" else "Llamada de voz"
        stickerUrl != null -> "Sticker"
        gifUrl != null -> "GIF"
        imageUrl != null -> "Imagen"
        audioUrl != null -> "Audio"
        else -> content.take(80)
    }
}
