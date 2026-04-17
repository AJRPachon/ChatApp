package com.ajrpachon.chatapp.ui.invitations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.domain.usecase.RespondInvitationUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ──────────────────────────────────────────────────────────────────

data class InvitationsState(
    val invitations: List<InvitationBO> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

// ── Intents ────────────────────────────────────────────────────────────────

sealed interface InvitationsIntent {
    data class Accept(val invitationId: String) : InvitationsIntent
    data class Reject(val invitationId: String) : InvitationsIntent
    data object DismissError : InvitationsIntent
}

// ── Effects ────────────────────────────────────────────────────────────────

sealed interface InvitationsEffect {
    data class ShowMessage(val text: String) : InvitationsEffect
}

// ── ViewModel ──────────────────────────────────────────────────────────────

class InvitationsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeInvitationsUseCase: ObserveInvitationsUseCase,
    private val respondInvitationUseCase: RespondInvitationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InvitationsState())
    val state = _state.asStateFlow()

    private val _effect = Channel<InvitationsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().filterNotNull().collectLatest { user ->
                observeInvitationsUseCase(user.id).collect { invitations ->
                    _state.update { it.copy(invitations = invitations, isLoading = false) }
                }
            }
        }
    }

    fun onIntent(intent: InvitationsIntent) {
        when (intent) {
            is InvitationsIntent.Accept -> respond(intent.invitationId, accept = true)
            is InvitationsIntent.Reject -> respond(intent.invitationId, accept = false)
            is InvitationsIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun respond(id: String, accept: Boolean) {
        viewModelScope.launch {
            val result = if (accept)
                respondInvitationUseCase.accept(id)
            else
                respondInvitationUseCase.reject(id)

            result
                .onSuccess {
                    val msg = if (accept) "Invitación aceptada" else "Invitación rechazada"
                    _effect.send(InvitationsEffect.ShowMessage(msg))
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }
}
