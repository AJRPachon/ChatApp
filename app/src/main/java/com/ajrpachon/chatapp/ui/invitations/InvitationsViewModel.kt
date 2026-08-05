package com.ajrpachon.chatapp.ui.invitations

import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.domain.usecase.RespondInvitationUseCase
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class InvitationsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeInvitationsUseCase: ObserveInvitationsUseCase,
    private val respondInvitationUseCase: RespondInvitationUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
) : BaseViewModel<InvitationsState, InvitationsEffect>(InvitationsState()) {

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().filterNotNull().collectLatest { user ->
                currentUserId = user.id
                catchResult {
                    observeInvitationsUseCase(user.id).collect { invitations ->
                        updateState { it.copy(invitations = invitations, isLoading = false) }
                    }
                }.onFailure { e ->
                    AppLogger.e(TAG, "Observe invitations failed", e)
                    updateState { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    fun onIntent(intent: InvitationsIntent) {
        when (intent) {
            is InvitationsIntent.Accept -> respond(intent.invitationId, accept = true)
            is InvitationsIntent.Reject -> respond(intent.invitationId, accept = false)
            is InvitationsIntent.DismissError -> updateState { it.copy(error = null) }
        }
    }

    private fun respond(id: String, accept: Boolean) {
        val invitation = state.value.invitations.firstOrNull { it.id == id } ?: return
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
                                sendEffect(InvitationsEffect.NavigateToChat(conv.id, invitation.sender.displayName))
                            }.onFailure { e ->
                                AppLogger.e(TAG, "Create conversation after accept failed", e)
                                sendEffect(InvitationsEffect.ShowMessage("Invitación aceptada"))
                            }
                        }
                    } else {
                        sendEffect(InvitationsEffect.ShowMessage("Invitación rechazada"))
                    }
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Respond invitation failed", e)
                    updateState { it.copy(error = e.message) }
                }
        }
    }

    companion object {
        private const val TAG = "InvitationsViewModel"
    }
}
