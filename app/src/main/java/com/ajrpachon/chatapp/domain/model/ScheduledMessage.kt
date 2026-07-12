package com.ajrpachon.chatapp.domain.model

data class ScheduledMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val scheduledAtMs: Long,
    val createdAt: Long,
)
