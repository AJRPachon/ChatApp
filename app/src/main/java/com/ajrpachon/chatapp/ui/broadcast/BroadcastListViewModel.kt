package com.ajrpachon.chatapp.ui.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.BroadcastListDao
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ── State / Intent / Effect ──────────────────────────────────────────────────

data class BroadcastListUiState(
    val lists: List<BroadcastListItem> = emptyList(),
    val isLoading: Boolean = true,
    // Create-dialog
    val showCreateDialog: Boolean = false,
    val newListName: String = "",
    val searchQuery: String = "",
    val searchResults: List<UserBO> = emptyList(),
    val selectedMembers: List<UserBO> = emptyList(),
    val isCreating: Boolean = false,
    // Send-dialog
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

// ── ViewModel ────────────────────────────────────────────────────────────────

class BroadcastListViewModel(
    private val broadcastListDao: BroadcastListDao,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BroadcastListUiState())
    val state = _state.asStateFlow()

    private val _effect = Channel<BroadcastListEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = catchResult {
                getCurrentUserUseCase().filterNotNull().first().id
            }.getOrNull()
        }
        viewModelScope.launch {
            broadcastListDao.observeAll().collect { dbLists ->
                val items = dbLists.map { dbo ->
                    val members = catchResult {
                        broadcastListDao.getMembersForList(dbo.id).map { u ->
                            UserBO(
                                id = u.id,
                                email = u.email,
                                username = u.username,
                                displayName = u.displayName,
                                avatarUrl = u.avatarUrl,
                                createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(u.createdAt),
                                lastSeen = u.lastSeen?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it) },
                                showOnlineStatus = u.showOnlineStatus,
                            )
                        }
                    }.getOrDefault(emptyList())
                    BroadcastListItem(id = dbo.id, name = dbo.name, createdAt = dbo.createdAt, members = members)
                }
                _state.update { it.copy(lists = items, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: BroadcastListIntent) {
        when (intent) {
            BroadcastListIntent.OpenCreateDialog ->
                _state.update { it.copy(showCreateDialog = true, newListName = "", searchQuery = "", searchResults = emptyList(), selectedMembers = emptyList()) }

            BroadcastListIntent.DismissCreateDialog ->
                _state.update { it.copy(showCreateDialog = false) }

            is BroadcastListIntent.NameChanged ->
                _state.update { it.copy(newListName = intent.name) }

            is BroadcastListIntent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }
                searchUsers(intent.query)
            }

            is BroadcastListIntent.ToggleMember -> {
                _state.update { current ->
                    val selected = current.selectedMembers.toMutableList()
                    if (selected.any { it.id == intent.user.id }) selected.removeAll { it.id == intent.user.id }
                    else selected.add(intent.user)
                    current.copy(selectedMembers = selected)
                }
            }

            BroadcastListIntent.CreateList -> createList()

            is BroadcastListIntent.DeleteList ->
                viewModelScope.launch {
                    catchResult { broadcastListDao.deleteWithMembers(intent.listId) }
                        .onFailure { e -> _state.update { it.copy(error = e.message) } }
                }

            is BroadcastListIntent.OpenSendDialog ->
                _state.update { it.copy(sendingListId = intent.listId, broadcastMessage = "") }

            BroadcastListIntent.DismissSendDialog ->
                _state.update { it.copy(sendingListId = null) }

            is BroadcastListIntent.BroadcastMessageChanged ->
                _state.update { it.copy(broadcastMessage = intent.message) }

            BroadcastListIntent.SendBroadcast -> sendBroadcast()

            BroadcastListIntent.DismissError ->
                _state.update { it.copy(error = null) }
        }
    }

    private fun searchUsers(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = catchResult { searchUsersUseCase(query) }.getOrDefault(emptyList())
            _state.update { it.copy(searchResults = results.filter { u -> u.id != currentUserId }) }
        }
    }

    private fun createList() {
        val current = _state.value
        if (current.newListName.isBlank() || current.selectedMembers.isEmpty()) {
            _state.update { it.copy(error = "Ingresa un nombre y selecciona al menos un miembro") }
            return
        }
        val uid = currentUserId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            val listId = UUID.randomUUID().toString()
            val dbo = BroadcastListDBO(id = listId, name = current.newListName.trim(), createdAt = System.currentTimeMillis())
            val members = current.selectedMembers.map { BroadcastListMemberDBO(listId = listId, userId = it.id) }
            catchResult { broadcastListDao.insertWithMembers(dbo, members) }
                .onSuccess { _state.update { it.copy(showCreateDialog = false) } }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Error al crear la lista") } }
            _state.update { it.copy(isCreating = false) }
        }
    }

    private fun sendBroadcast() {
        val current = _state.value
        val listId = current.sendingListId ?: return
        val message = current.broadcastMessage.trim()
        if (message.isBlank()) {
            _state.update { it.copy(error = "Escribe un mensaje para difundir") }
            return
        }
        val uid = currentUserId ?: return
        val listItem = current.lists.find { it.id == listId } ?: return
        if (listItem.members.isEmpty()) {
            _state.update { it.copy(error = "La lista no tiene miembros") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            var failCount = 0
            for (member in listItem.members) {
                catchResult {
                    val conv = getOrCreateConversationUseCase(uid, member.id)
                    sendMessageUseCase(
                        conversationId = conv.id,
                        senderId = uid,
                        content = message,
                        otherUserId = member.id,
                    )
                }.onFailure { e ->
                    failCount++
                    AppLogger.e("BroadcastListViewModel", "Failed to send to ${member.id}", e)
                }
            }
            _state.update { it.copy(isSending = false, sendingListId = null) }
            val sentCount = listItem.members.size - failCount
            _effect.send(BroadcastListEffect.ShowToast("Enviado a $sentCount/${listItem.members.size} contactos"))
        }
    }
}
