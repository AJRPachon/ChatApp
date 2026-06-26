package com.ajrpachon.chatapp.domain.model

import kotlinx.datetime.Instant

data class UserBO(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val createdAt: Instant,
    val lastSeen: Instant? = null,
    val showOnlineStatus: Boolean = true,
) {
    fun isOnline(): Boolean =
        showOnlineStatus &&
        lastSeen != null &&
        (System.currentTimeMillis() - lastSeen.toEpochMilliseconds()) < ONLINE_THRESHOLD_MS

    companion object {
        const val ONLINE_THRESHOLD_MS = 3 * 60 * 1000L
    }
}
