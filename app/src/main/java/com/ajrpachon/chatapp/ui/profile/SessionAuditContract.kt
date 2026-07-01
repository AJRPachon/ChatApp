package com.ajrpachon.chatapp.ui.profile

data class SessionInfo(
    val id: String,
    val deviceInfo: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isCurrent: Boolean,
)

data class SessionAuditState(
    val sessions: List<SessionInfo> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface SessionAuditIntent {
    data class RevokeSession(val sessionId: String) : SessionAuditIntent
    data object RevokeAllOtherSessions : SessionAuditIntent
    data object Refresh : SessionAuditIntent
}

sealed interface SessionAuditEffect {
    data object SessionRevoked : SessionAuditEffect
    data class Error(val message: String) : SessionAuditEffect
}
