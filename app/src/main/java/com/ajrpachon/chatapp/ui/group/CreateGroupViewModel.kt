package com.ajrpachon.chatapp.ui.group
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.usecase.CreateGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupState())
    val state = _state.asStateFlow()

    private val _effect = Channel<CreateGroupEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

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
                _state.update { it.copy(query = intent.query) }
                search(intent.query)
            }
            is CreateGroupIntent.ToggleUser -> toggleUser(intent.user)
            is CreateGroupIntent.Next -> {
                if (_state.value.selectedUsers.isNotEmpty()) {
                    _state.update { it.copy(step = CreateGroupStep.SET_INFO) }
                }
            }
            is CreateGroupIntent.Back -> {
                if (_state.value.step == CreateGroupStep.SET_INFO) {
                    _state.update { it.copy(step = CreateGroupStep.SELECT_MEMBERS) }
                } else {
                    viewModelScope.launch { _effect.send(CreateGroupEffect.GoBack) }
                }
            }
            is CreateGroupIntent.NameChanged -> _state.update { it.copy(groupName = intent.name) }
            is CreateGroupIntent.DescriptionChanged -> _state.update { it.copy(groupDescription = intent.description) }
            is CreateGroupIntent.Create -> createGroup()
            is CreateGroupIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = catchResult { searchUsersUseCase(query) }.getOrDefault(emptyList())
            _state.update { currentState ->
                currentState.copy(searchResults = results.filter { it.id != currentUserId })
            }
        }
    }

    private fun toggleUser(user: UserBO) {
        _state.update { currentState ->
            val selected = currentState.selectedUsers.toMutableList()
            if (selected.any { it.id == user.id }) selected.removeAll { it.id == user.id }
            else selected.add(user)
            currentState.copy(selectedUsers = selected)
        }
    }

    private fun createGroup() {
        val userId = currentUserId ?: return
        val currentState = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            createGroupUseCase(
                name = currentState.groupName,
                description = currentState.groupDescription.takeIf { it.isNotBlank() },
                createdBy = userId,
                participantIds = currentState.selectedUsers.map { it.id },
            ).onSuccess { conv ->
                _effect.send(CreateGroupEffect.NavigateToChat(conv.id, conv.name))
            }.onFailure { e ->
                _state.update { it.copy(error = e.message ?: "Error al crear el grupo") }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}
