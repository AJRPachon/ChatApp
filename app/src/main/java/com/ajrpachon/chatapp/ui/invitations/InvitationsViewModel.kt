package com.ajrpachon.chatapp.ui.invitations

import com.ajrpachon.chatapp.domain.usecase.CancelSentInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetSentInvitationsUseCase
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
    private val getSentInvitationsUseCase: GetSentInvitationsUseCase,
    private val cancelSentInvitationUseCase: CancelSentInvitationUseCase,
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
            is InvitationsIntent.SelectTab -> selectTab(intent.tab)
            is InvitationsIntent.CancelSent -> cancelSent(intent.invitationId)
            is InvitationsIntent.DismissError -> updateState { it.copy(error = null) }
        }
    }

    private fun selectTab(tab: InvitationsTab) {
        updateState { it.copy(selectedTab = tab) }
        if (tab == InvitationsTab.SENT) loadSentInvitations()
    }

    private fun loadSentInvitations() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            updateState { it.copy(isSentLoading = true) }
            getSentInvitationsUseCase(uid)
                .onSuccess { sent -> updateState { it.copy(sentInvitations = sent, isSentLoading = false) } }
                .onFailure { e ->
                    AppLogger.e(TAG, "Get sent invitations failed", e)
                    updateState { it.copy(isSentLoading = false, error = e.message) }
                }
        }
    }

    private fun cancelSent(id: String) {
        viewModelScope.launch {
            cancelSentInvitationUseCase(id)
                .onSuccess {
                    sendEffect(InvitationsEffect.ShowMessage("Invitación cancelada"))
                    loadSentInvitations()
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Cancel sent invitation failed", e)
                    updateState { it.copy(error = e.message) }
                }
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
                    // Accepting just marks the invitation accepted and stays on this screen — no
                    // auto-navigation into the new chat. The conversation still gets created
                    // lazily the next time either side opens it (SendInvitationUseCase already
                    // handles UserRelationship.CONNECTED that way from New Chat).
                    val message = if (accept) "Invitación aceptada" else "Invitación rechazada"
                    sendEffect(InvitationsEffect.ShowMessage(message))
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
