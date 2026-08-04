package com.ajrpachon.chatapp.ui.broadcast

import com.ajrpachon.chatapp.domain.model.UserBO

data class BroadcastListUiState(
    val lists: List<BroadcastListItem> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val newListName: String = "",
    val searchQuery: String = "",
    val searchResults: List<UserBO> = emptyList(),
    val selectedMembers: List<UserBO> = emptyList(),
    val selectedMemberIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val sendingListId: String? = null,
    val broadcastMessage: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
)

data class BroadcastListItem(
    val id: String,
    val name: String,
    val createdAt: Long,
    val members: List<UserBO> = emptyList(),
)

sealed interface BroadcastListIntent {
    data object OpenCreateDialog : BroadcastListIntent
    data object DismissCreateDialog : BroadcastListIntent
    data class NameChanged(val name: String) : BroadcastListIntent
    data class SearchQueryChanged(val query: String) : BroadcastListIntent
    data class ToggleMember(val user: UserBO) : BroadcastListIntent
    data object CreateList : BroadcastListIntent
    data class DeleteList(val listId: String) : BroadcastListIntent
    data class OpenSendDialog(val listId: String) : BroadcastListIntent
    data object DismissSendDialog : BroadcastListIntent
    data class BroadcastMessageChanged(val message: String) : BroadcastListIntent
    data object SendBroadcast : BroadcastListIntent
    data object DismissError : BroadcastListIntent
}

sealed interface BroadcastListEffect {
    data object GoBack : BroadcastListEffect
    data class ShowToast(val message: String) : BroadcastListEffect
}
