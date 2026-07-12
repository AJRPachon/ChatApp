package com.ajrpachon.chatapp.ui.conversations

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.NotificationSound
import com.ajrpachon.chatapp.domain.model.ThemePreference

enum class ConversationFilter { ALL, UNREAD, GROUPS, DIRECT }

data class ConversationListState(
    val conversations: List<ConversationBO> = emptyList(),
    val archivedConversations: List<ConversationBO> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val pendingInvitationsCount: Int = 0,
    val error: String? = null,
    val sortByUnread: Boolean = false,
    val selectedFilter: ConversationFilter = ConversationFilter.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showArchivedSheet: Boolean = false,
    val drafts: Map<String, String> = emptyMap(),
    val soundPickerConversationId: String? = null,
    val isOnline: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
) {
    val filteredConversations: List<ConversationBO>
        get() {
            val bySearch = if (searchQuery.isBlank()) conversations
            else conversations.filter { it.name.contains(searchQuery, ignoreCase = true) }
            return when (selectedFilter) {
                ConversationFilter.ALL -> bySearch
                ConversationFilter.UNREAD -> bySearch.filter { it.unreadCount > 0 }
                ConversationFilter.GROUPS -> bySearch.filter { it.isGroup }
                ConversationFilter.DIRECT -> bySearch.filter { !it.isGroup }
            }
        }
}

sealed interface ConversationListIntent {
    data class OpenConversation(val conversationId: String, val conversationName: String, val isGroup: Boolean) : ConversationListIntent
    data object DismissError : ConversationListIntent
    data class DeleteConversation(val conversationId: String) : ConversationListIntent
    data class ToggleMute(val conversationId: String, val muted: Boolean) : ConversationListIntent
    data class ClearChat(val conversationId: String) : ConversationListIntent
    data class LeaveGroup(val conversationId: String) : ConversationListIntent
    data object ToggleSortByUnread : ConversationListIntent
    data class SetFilter(val filter: ConversationFilter) : ConversationListIntent
    data class SearchQueryChanged(val query: String) : ConversationListIntent
    data object ToggleSearch : ConversationListIntent
    data class ArchiveConversation(val conversationId: String, val archived: Boolean) : ConversationListIntent
    data object ShowArchivedSheet : ConversationListIntent
    data object DismissArchivedSheet : ConversationListIntent
    data class ShowSoundPicker(val conversationId: String) : ConversationListIntent
    data object DismissSoundPicker : ConversationListIntent
    data class SetNotificationSound(val conversationId: String, val sound: NotificationSound) : ConversationListIntent
    data class SetTheme(val theme: ThemePreference) : ConversationListIntent
}

sealed interface ConversationListEffect {
    data class NavigateToChat(val conversationId: String, val conversationName: String, val isGroup: Boolean) : ConversationListEffect
}
