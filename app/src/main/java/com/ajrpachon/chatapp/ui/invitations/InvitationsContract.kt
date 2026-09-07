package com.ajrpachon.chatapp.ui.invitations

import com.ajrpachon.chatapp.domain.model.InvitationBO

enum class InvitationsTab { RECEIVED, SENT }

data class InvitationsState(
    val invitations: List<InvitationBO> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: InvitationsTab = InvitationsTab.RECEIVED,
    val sentInvitations: List<InvitationBO> = emptyList(),
    val isSentLoading: Boolean = false,
)

sealed interface InvitationsIntent {
    data class Accept(val invitationId: String) : InvitationsIntent
    data class Reject(val invitationId: String) : InvitationsIntent
    data class SelectTab(val tab: InvitationsTab) : InvitationsIntent
    data class CancelSent(val invitationId: String) : InvitationsIntent
    data object DismissError : InvitationsIntent
}

sealed interface InvitationsEffect {
    data class ShowMessage(val text: String) : InvitationsEffect
    data class NavigateToChat(val conversationId: String, val name: String) : InvitationsEffect
}
