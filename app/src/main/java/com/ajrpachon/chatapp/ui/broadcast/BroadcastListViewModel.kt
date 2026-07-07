package com.ajrpachon.chatapp.ui.broadcast

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.BroadcastListDao
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.domain.model.UserBO
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
) : BaseViewModel<BroadcastListUiState, BroadcastListEffect>(BroadcastListUiState()) {

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
                updateState { it.copy(lists = items, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: BroadcastListIntent) {
        when (intent) {
            BroadcastListIntent.OpenCreateDialog ->
                updateState { it.copy(showCreateDialog = true, newListName = "", searchQuery = "", searchResults = emptyList(), selectedMembers = emptyList()) }

            BroadcastListIntent.DismissCreateDialog ->
                updateState { it.copy(showCreateDialog = false) }

            is BroadcastListIntent.NameChanged ->
                updateState { it.copy(newListName = intent.name) }

            is BroadcastListIntent.SearchQueryChanged -> {
                updateState { it.copy(searchQuery = intent.query) }
                searchUsers(intent.query)
            }

            is BroadcastListIntent.ToggleMember -> {
                updateState { current ->
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
                        .onFailure { e -> updateState { it.copy(error = e.message) } }
                }

            is BroadcastListIntent.OpenSendDialog ->
                updateState { it.copy(sendingListId = intent.listId, broadcastMessage = "") }

            BroadcastListIntent.DismissSendDialog ->
                updateState { it.copy(sendingListId = null) }

            is BroadcastListIntent.BroadcastMessageChanged ->
                updateState { it.copy(broadcastMessage = intent.message) }

            BroadcastListIntent.SendBroadcast -> sendBroadcast()

            BroadcastListIntent.DismissError ->
                updateState { it.copy(error = null) }
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
        val current = state.value
        if (current.newListName.isBlank() || current.selectedMembers.isEmpty()) {
            updateState { it.copy(error = "Ingresa un nombre y selecciona al menos un miembro") }
            return
        }
        currentUserId ?: return
        viewModelScope.launch {
            updateState { it.copy(isCreating = true) }
            val listId = UUID.randomUUID().toString()
            val dbo = BroadcastListDBO(id = listId, name = current.newListName.trim(), createdAt = System.currentTimeMillis())
            val members = current.selectedMembers.map { BroadcastListMemberDBO(listId = listId, userId = it.id) }
            catchResult { broadcastListDao.insertWithMembers(dbo, members) }
                .onSuccess { updateState { it.copy(showCreateDialog = false) } }
                .onFailure { e -> updateState { it.copy(error = e.message ?: "Error al crear la lista") } }
            updateState { it.copy(isCreating = false) }
        }
    }

    private fun sendBroadcast() {
        val current = state.value
        val listId = current.sendingListId ?: return
        val message = current.broadcastMessage.trim()
        if (message.isBlank()) {
            updateState { it.copy(error = "Escribe un mensaje para difundir") }
            return
        }
        val uid = currentUserId ?: return
        val listItem = current.lists.find { it.id == listId } ?: return
        if (listItem.members.isEmpty()) {
            updateState { it.copy(error = "La lista no tiene miembros") }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isSending = true) }
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
            updateState { it.copy(isSending = false, sendingListId = null) }
            val sentCount = listItem.members.size - failCount
            sendEffect(BroadcastListEffect.ShowToast("Enviado a $sentCount/${listItem.members.size} contactos"))
        }
    }
}
