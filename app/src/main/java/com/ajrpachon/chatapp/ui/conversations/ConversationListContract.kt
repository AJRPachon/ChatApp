package com.ajrpachon.chatapp.ui.conversations

import com.ajrpachon.chatapp.domain.model.ConversationBO

data class ConversationListState(
    val conversations: List<ConversationBO> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val pendingInvitationsCount: Int = 0,
    val error: String? = null,
)

sealed interface ConversationListIntent {
    data class OpenConversation(val conversationId: String, val conversationName: String, val isGroup: Boolean) : ConversationListIntent
    data object DismissError : ConversationListIntent
    data class DeleteConversation(val conversationId: String) : ConversationListIntent
    data class ToggleMute(val conversationId: String, val muted: Boolean) : ConversationListIntent
    data class ClearChat(val conversationId: String) : ConversationListIntent
    data class LeaveGroup(val conversationId: String) : ConversationListIntent
}

sealed interface ConversationListEffect {
    data class NavigateToChat(val conversationId: String, val conversationName: String, val isGroup: Boolean) : ConversationListEffect
}
