package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class StatusBO(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val text: String?,
    val imageUrl: String?,
    val backgroundColor: Long,
    val createdAt: Instant,
    val expiresAt: Instant,
    val isFromMe: Boolean,
) {
    fun isExpired(): Boolean = expiresAt.toEpochMilliseconds() <= System.currentTimeMillis()
}
