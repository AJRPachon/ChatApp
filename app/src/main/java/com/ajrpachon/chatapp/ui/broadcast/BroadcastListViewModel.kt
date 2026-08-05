package com.ajrpachon.chatapp.ui.broadcast

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.BroadcastListBO
import com.ajrpachon.chatapp.domain.repository.BroadcastListRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class BroadcastListViewModel(
    private val broadcastListRepository: BroadcastListRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
) : BaseViewModel<BroadcastListUiState, BroadcastListEffect>(BroadcastListUiState()) {

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = catchResult {
                getCurrentUserUseCase().filterNotNull().first().id
            }.getOrNull()
        }
        viewModelScope.launch {
            broadcastListRepository.observeAll().collect { bos ->
                updateState { it.copy(lists = bos.map { bo -> bo.toItem() }, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: BroadcastListIntent) {
        when (intent) {
            BroadcastListIntent.OpenCreateDialog -> updateState {
                it.copy(
                    showCreateDialog = true,
                    newListName = "",
                    searchQuery = "",
                    searchResults = emptyList(),
                    selectedMembers = emptyList(),
                    selectedMemberIds = emptySet(),
                )
            }
            BroadcastListIntent.DismissCreateDialog -> updateState { it.copy(showCreateDialog = false) }
            is BroadcastListIntent.NameChanged -> updateState { it.copy(newListName = intent.name) }
            is BroadcastListIntent.SearchQueryChanged -> {
                updateState { it.copy(searchQuery = intent.query) }
                searchUsers(intent.query)
            }
            is BroadcastListIntent.ToggleMember -> updateState { cur ->
                val selected = cur.selectedMembers.toMutableList()
                if (selected.any { it.id == intent.user.id }) {
                    selected.removeAll { it.id == intent.user.id }
                } else {
                    selected.add(intent.user)
                }
                cur.copy(
                    selectedMembers = selected,
                    selectedMemberIds = selected.map { it.id }.toSet(),
                )
            }
            BroadcastListIntent.CreateList -> createList()
            is BroadcastListIntent.DeleteList -> viewModelScope.launch {
                catchResult { broadcastListRepository.delete(intent.listId) }
                    .onFailure { e -> updateState { it.copy(error = e.message) } }
            }
            is BroadcastListIntent.OpenSendDialog -> updateState {
                it.copy(sendingListId = intent.listId, broadcastMessage = "")
            }
            BroadcastListIntent.DismissSendDialog -> updateState { it.copy(sendingListId = null) }
            is BroadcastListIntent.BroadcastMessageChanged -> updateState {
                it.copy(broadcastMessage = intent.message)
            }
            BroadcastListIntent.SendBroadcast -> sendBroadcast()
            BroadcastListIntent.DismissError -> updateState { it.copy(error = null) }
        }
    }

    private fun searchUsers(query: String) {
        if (query.isBlank()) {
            updateState { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = catchResult { searchUsersUseCase(query) }.getOrDefault(emptyList())
            updateState { it.copy(searchResults = results.filter { u -> u.id != currentUserId }) }
        }
    }

    private fun createList() {
        val cur = state.value
        if (cur.newListName.isBlank() || cur.selectedMembers.isEmpty()) {
            updateState { it.copy(error = "Ingresa un nombre y selecciona al menos un miembro") }
            return
        }
        currentUserId ?: return
        viewModelScope.launch {
            updateState { it.copy(isCreating = true) }
            catchResult {
                broadcastListRepository.create(
                    id = UUID.randomUUID().toString(),
                    name = cur.newListName.trim(),
                    memberIds = cur.selectedMembers.map { it.id },
                    createdAt = System.currentTimeMillis(),
                )
            }
                .onSuccess { updateState { it.copy(showCreateDialog = false) } }
                .onFailure { e -> updateState { it.copy(error = e.message ?: "Error") } }
            updateState { it.copy(isCreating = false) }
        }
    }

    private fun sendBroadcast() {
        val cur = state.value
        val listId = cur.sendingListId ?: return
        val msg = cur.broadcastMessage.trim()
        if (msg.isBlank()) {
            updateState { it.copy(error = "Escribe un mensaje") }
            return
        }
        val uid = currentUserId ?: return
        val li = cur.lists.find { it.id == listId } ?: return
        if (li.members.isEmpty()) {
            updateState { it.copy(error = "Sin miembros") }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isSending = true) }
            var failures = 0
            for (member in li.members) {
                catchResult {
                    val conversation = getOrCreateConversationUseCase(uid, member.id)
                    sendMessageUseCase(
                        conversationId = conversation.id,
                        senderId = uid,
                        content = msg,
                        otherUserId = member.id,
                    )
                }.onFailure { e ->
                    failures++
                    AppLogger.e("BroadcastListVM", "fail ${member.id}", e)
                }
            }
            updateState { it.copy(isSending = false, sendingListId = null) }
            sendEffect(
                BroadcastListEffect.ShowToast(
                    "Enviado a ${li.members.size - failures}/${li.members.size} contactos"
                )
            )
        }
    }
}

private fun BroadcastListBO.toItem() = BroadcastListItem(
    id = id,
    name = name,
    createdAt = createdAt,
    members = members,
)
