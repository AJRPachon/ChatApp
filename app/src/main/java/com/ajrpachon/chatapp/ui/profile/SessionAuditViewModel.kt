package com.ajrpachon.chatapp.ui.profile

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.SessionDao
import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import com.ajrpachon.chatapp.utils.catchResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SessionAuditViewModel(
    private val supabase: SupabaseClient,
    private val sessionDao: SessionDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionAuditState())
    val state = _state.asStateFlow()

    private val _effect = Channel<SessionAuditEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        ensureCurrentSessionRecorded()
        observeSessions()
    }

    private fun ensureCurrentSessionRecorded() {
        viewModelScope.launch {
            val session = supabase.auth.currentSession ?: return@launch
            val now = System.currentTimeMillis()
            val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            sessionDao.updateCurrentLastActive(now)
            sessionDao.upsert(
                SessionDBO(
                    id = session.accessToken.take(36).let { token ->
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
        sessionDao.observeAll()
            .onEach { list ->
                _state.update { s ->
                    s.copy(
                        sessions = list.map { dbo ->
                            SessionInfo(
                                id = dbo.id,
                                deviceInfo = dbo.deviceInfo,
                                createdAt = dbo.createdAt,
                                lastActiveAt = dbo.lastActiveAt,
                                isCurrent = dbo.isCurrent,
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
            val isCurrent = _state.value.sessions.find { it.id == sessionId }?.isCurrent == true
            catchResult {
                if (isCurrent) {
                    supabase.auth.signOut()
                    sessionDao.deleteAll()
                } else {
                    sessionDao.delete(sessionId)
                }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
                _effect.send(SessionAuditEffect.Error(e.message ?: "Error al revocar sesión"))
                return@launch
            }
            _effect.send(SessionAuditEffect.SessionRevoked)
        }
    }

    private fun revokeAllOthers() {
        viewModelScope.launch {
            catchResult {
                sessionDao.deleteAllOthers()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
                _effect.send(SessionAuditEffect.Error(e.message ?: "Error al cerrar otras sesiones"))
                return@launch
            }
            _effect.send(SessionAuditEffect.SessionRevoked)
        }
    }
}
