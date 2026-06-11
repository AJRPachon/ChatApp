package com.ajrpachon.chatapp.ui.group

import com.ajrpachon.chatapp.domain.model.UserBO

enum class CreateGroupStep { SELECT_MEMBERS, SET_INFO }

data class CreateGroupState(
    val step: CreateGroupStep = CreateGroupStep.SELECT_MEMBERS,
    val query: String = "",
    val searchResults: List<UserBO> = emptyList(),
    val selectedUsers: List<UserBO> = emptyList(),
    val groupName: String = "",
    val groupDescription: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface CreateGroupIntent {
    data class QueryChanged(val query: String) : CreateGroupIntent
    data class ToggleUser(val user: UserBO) : CreateGroupIntent
    data object Next : CreateGroupIntent
    data object Back : CreateGroupIntent
    data class NameChanged(val name: String) : CreateGroupIntent
    data class DescriptionChanged(val description: String) : CreateGroupIntent
    data object Create : CreateGroupIntent
    data object DismissError : CreateGroupIntent
}

sealed interface CreateGroupEffect {
    data class NavigateToChat(val conversationId: String, val name: String) : CreateGroupEffect
    data object GoBack : CreateGroupEffect
}
