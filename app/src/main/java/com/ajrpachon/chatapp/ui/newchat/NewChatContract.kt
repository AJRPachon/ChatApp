package com.ajrpachon.chatapp.ui.newchat

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship

data class NewChatState(
    val query: String = "",
    val currentUsername: String = "",
    val currentUserId: String = "",
    val appUsers: List<UserBO> = emptyList(),
    val contacts: List<PhoneContact> = emptyList(),
    val isLoadingUsers: Boolean = false,
    val contactsPermissionDenied: Boolean = false,
    val error: String? = null,
    val userRelationships: Map<String, UserRelationship> = emptyMap(),
    val pendingUserIds: Set<String> = emptySet(),
)

sealed interface NewChatIntent {
    data class QueryChanged(val query: String) : NewChatIntent
    data class ContactsLoaded(val contacts: List<PhoneContact>) : NewChatIntent
    data object ContactsPermissionDenied : NewChatIntent
    data class UserAction(val otherUser: UserBO) : NewChatIntent
    data class BlockUser(val otherUser: UserBO) : NewChatIntent
    data class UnblockUser(val otherUser: UserBO) : NewChatIntent
    data object DismissError : NewChatIntent
}

sealed interface NewChatEffect {
    data class NavigateToChat(val conversationId: String, val otherUserName: String) : NewChatEffect
    data object NavigateToInvitations : NewChatEffect
    data class ShowMessage(val text: String) : NewChatEffect
}
