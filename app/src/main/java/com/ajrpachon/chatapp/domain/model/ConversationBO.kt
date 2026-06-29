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
    val trailingImageCount: Int = 0,
    val otherUserAvatarUrl: String? = null,
    val groupAvatarUrl: String? = null,
    val description: String? = null,
    val isMuted: Boolean = false,
    val mutedUntil: Long = 0L,
    val isArchived: Boolean = false,
) {
    val displayAvatarUrl: String? get() = if (isGroup) groupAvatarUrl else otherUserAvatarUrl
}
