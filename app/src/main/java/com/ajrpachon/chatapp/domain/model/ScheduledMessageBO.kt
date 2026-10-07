package com.ajrpachon.chatapp.domain.model

data class ScheduledMessageBO(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val scheduledAtMs: Long,
    val createdAt: Long,
)
