package com.ajrpachon.chatapp.ui.invitations
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.domain.usecase.RespondInvitationUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvitationsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeInvitationsUseCase: ObserveInvitationsUseCase,
    private val respondInvitationUseCase: RespondInvitationUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InvitationsState())
    val state = _state.asStateFlow()

    private val _effect = Channel<InvitationsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().filterNotNull().collectLatest { user ->
                currentUserId = user.id
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
        val invitation = _state.value.invitations.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val result = if (accept)
                respondInvitationUseCase.accept(id)
            else
                respondInvitationUseCase.reject(id)

            result
                .onSuccess {
                    if (accept) {
                        val uid = currentUserId
                        if (uid != null) {
                            catchResult {
                                val conv = getOrCreateConversationUseCase(uid, invitation.sender.id)
                                _effect.send(InvitationsEffect.NavigateToChat(conv.id, invitation.sender.displayName))
                            }.onFailure { e ->
                                AppLogger.e(TAG, "Create conversation after accept failed", e)
                                _effect.send(InvitationsEffect.ShowMessage("Invitación aceptada"))
                            }
                        }
                    } else {
                        _effect.send(InvitationsEffect.ShowMessage("Invitación rechazada"))
                    }
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Respond invitation failed", e)
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    companion object {
        private const val TAG = "InvitationsViewModel"
    }
}
