package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class GroupMemberBO(
    val userId: String,
    val conversationId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val role: GroupRole,
    val joinedAt: Instant,
)
