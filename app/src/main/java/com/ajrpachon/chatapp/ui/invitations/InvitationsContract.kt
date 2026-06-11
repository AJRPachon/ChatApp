package com.ajrpachon.chatapp.ui.invitations

import com.ajrpachon.chatapp.domain.model.InvitationBO

data class InvitationsState(
    val invitations: List<InvitationBO> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

sealed interface InvitationsIntent {
    data class Accept(val invitationId: String) : InvitationsIntent
    data class Reject(val invitationId: String) : InvitationsIntent
    data object DismissError : InvitationsIntent
}

sealed interface InvitationsEffect {
    data class ShowMessage(val text: String) : InvitationsEffect
    data class NavigateToChat(val conversationId: String, val name: String) : InvitationsEffect
}
