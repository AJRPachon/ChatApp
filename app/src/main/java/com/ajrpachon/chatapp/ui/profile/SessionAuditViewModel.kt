package com.ajrpachon.chatapp.ui.profile

import android.os.Build
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.model.SessionBO
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.catchResult
import com.ajrpachon.chatapp.domain.repository.SessionRepository

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class SessionAuditViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : BaseViewModel<SessionAuditState, SessionAuditEffect>(SessionAuditState()) {

    init {
        ensureCurrentSessionRecorded()
        observeSessions()
    }

    private fun ensureCurrentSessionRecorded() {
        viewModelScope.launch {
            val session = authRepository.getCurrentSessionInfo() ?: return@launch
            val now = System.currentTimeMillis()
            val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            sessionRepository.updateCurrentLastActive(now)
            sessionRepository.upsert(
                SessionBO(
                    id = session.userId.take(36).let { token ->
                        runCatching { UUID.nameUUIDFromBytes(token.toByteArray()).toString() }
                            .getOrDefault(token.padEnd(36, '0').take(36))
                    },
                    deviceInfo = deviceLabel,
                    createdAt = now,
                    lastActiveAt = now,
                    isCurrent = true,
                )
            )
        }
    }

    private fun observeSessions() {
        sessionRepository.observeAll()
            .onEach { list ->
                updateState { s ->
                    s.copy(
                        sessions = list.map { bo ->
                            SessionInfo(
                                id = bo.id,
                                deviceInfo = bo.deviceInfo,
                                createdAt = bo.createdAt,
                                lastActiveAt = bo.lastActiveAt,
                                isCurrent = bo.isCurrent,
                            )
                        },
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SessionAuditIntent) {
        when (intent) {
            is SessionAuditIntent.RevokeSession -> revokeSession(intent.sessionId)
            SessionAuditIntent.RevokeAllOtherSessions -> revokeAllOthers()
            SessionAuditIntent.Refresh -> {
                ensureCurrentSessionRecorded()
            }
        }
    }

    private fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            val isCurrent = state.value.sessions.find { it.id == sessionId }?.isCurrent == true
            catchResult {
                if (isCurrent) {
                    authRepository.signOut()
                    sessionRepository.deleteAll()
                } else {
                    sessionRepository.delete(sessionId)
                }
            }.onFailure { e ->
                updateState { it.copy(error = e.message) }
                sendEffect(SessionAuditEffect.Error(e.message ?: "Error al revocar sesión"))
                return@launch
            }
            sendEffect(SessionAuditEffect.SessionRevoked)
        }
    }

    private fun revokeAllOthers() {
        viewModelScope.launch {
            catchResult {
                sessionRepository.deleteAllOthers()
            }.onFailure { e ->
                updateState { it.copy(error = e.message) }
                sendEffect(SessionAuditEffect.Error(e.message ?: "Error al cerrar otras sesiones"))
                return@launch
            }
            sendEffect(SessionAuditEffect.SessionRevoked)
        }
    }
}
