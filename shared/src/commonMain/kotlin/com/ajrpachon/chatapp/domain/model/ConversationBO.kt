package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class ConversationBO(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val participants: List<UserBO>,
    val lastMessage: MessageBO?,
    val unreadCount: Int,
    val updatedAt: Instant,
)
