package com.ajrpachon.chatapp.ui.conversations

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.NotificationSound

data class ConversationListState(
    val conversations: List<ConversationBO> = emptyList(),
    val archivedConversations: List<ConversationBO> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val pendingInvitationsCount: Int = 0,
    val error: String? = null,
    val sortByUnread: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showArchivedSheet: Boolean = false,
    val drafts: Map<String, String> = emptyMap(),
    val soundPickerConversationId: String? = null,
) {
    val filteredConversations: List<ConversationBO>
        get() = if (searchQuery.isBlank()) conversations
        else conversations.filter { it.name.contains(searchQuery, ignoreCase = true) }
}

sealed interface ConversationListIntent {
    data class OpenConversation(val conversationId: String, val conversationName: String, val isGroup: Boolean) : ConversationListIntent
    data object DismissError : ConversationListIntent
    data class DeleteConversation(val conversationId: String) : ConversationListIntent
    data class ToggleMute(val conversationId: String, val muted: Boolean) : ConversationListIntent
    data class ClearChat(val conversationId: String) : ConversationListIntent
    data class LeaveGroup(val conversationId: String) : ConversationListIntent
    data object ToggleSortByUnread : ConversationListIntent
    data class SearchQueryChanged(val query: String) : ConversationListIntent
    data object ToggleSearch : ConversationListIntent
    data class ArchiveConversation(val conversationId: String, val archived: Boolean) : ConversationListIntent
    data object ShowArchivedSheet : ConversationListIntent
    data object DismissArchivedSheet : ConversationListIntent
    data class ShowSoundPicker(val conversationId: String) : ConversationListIntent
    data object DismissSoundPicker : ConversationListIntent
    data class SetNotificationSound(val conversationId: String, val sound: NotificationSound) : ConversationListIntent
}

sealed interface ConversationListEffect {
    data class NavigateToChat(val conversationId: String, val conversationName: String, val isGroup: Boolean) : ConversationListEffect
}
