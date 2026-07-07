package com.ajrpachon.chatapp.ui.group
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.usecase.CreateGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
) : BaseViewModel<CreateGroupState, CreateGroupEffect>(CreateGroupState()) {

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = catchResult {
                getCurrentUserUseCase().filterNotNull().first().id
            }.getOrNull()
        }
    }

    fun onIntent(intent: CreateGroupIntent) {
        when (intent) {
            is CreateGroupIntent.QueryChanged -> {
                updateState { it.copy(query = intent.query) }
                search(intent.query)
            }
            is CreateGroupIntent.ToggleUser -> toggleUser(intent.user)
            is CreateGroupIntent.Next -> {
                if (state.value.selectedUsers.isNotEmpty()) {
                    updateState { it.copy(step = CreateGroupStep.SET_INFO) }
                }
            }
            is CreateGroupIntent.Back -> {
                if (state.value.step == CreateGroupStep.SET_INFO) {
                    updateState { it.copy(step = CreateGroupStep.SELECT_MEMBERS) }
                } else {
                    viewModelScope.launch { sendEffect(CreateGroupEffect.GoBack) }
                }
            }
            is CreateGroupIntent.NameChanged -> updateState { it.copy(groupName = intent.name) }
            is CreateGroupIntent.DescriptionChanged -> updateState { it.copy(groupDescription = intent.description) }
            is CreateGroupIntent.Create -> createGroup()
            is CreateGroupIntent.DismissError -> updateState { it.copy(error = null) }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            updateState { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = catchResult { searchUsersUseCase(query) }.getOrDefault(emptyList())
            updateState { currentState ->
                currentState.copy(searchResults = results.filter { it.id != currentUserId })
            }
        }
    }

    private fun toggleUser(user: UserBO) {
        updateState { currentState ->
            val selected = currentState.selectedUsers.toMutableList()
            if (selected.any { it.id == user.id }) selected.removeAll { it.id == user.id }
            else selected.add(user)
            currentState.copy(selectedUsers = selected)
        }
    }

    private fun createGroup() {
        val userId = currentUserId ?: return
        val currentState = state.value
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            createGroupUseCase(
                name = currentState.groupName,
                description = currentState.groupDescription.takeIf { it.isNotBlank() },
                createdBy = userId,
                participantIds = currentState.selectedUsers.map { it.id },
            ).onSuccess { conv ->
                sendEffect(CreateGroupEffect.NavigateToChat(conv.id, conv.name))
            }.onFailure { e ->
                updateState { it.copy(error = e.message ?: "Error al crear el grupo") }
            }
            updateState { it.copy(isLoading = false) }
        }
    }
}
