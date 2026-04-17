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
)
