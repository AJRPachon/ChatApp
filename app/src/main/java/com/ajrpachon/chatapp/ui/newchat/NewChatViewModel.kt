package com.ajrpachon.chatapp.ui.newchat
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.utils.ContactSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ajrpachon.chatapp.domain.usecase.BlockUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationResult
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewChatViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val sendInvitationUseCase: SendInvitationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val userRepository: UserRepository,
    private val contactSyncManager: ContactSyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(NewChatState())
    val state = _state.asStateFlow()

    private val _effect = Channel<NewChatEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                _state.update { it.copy(currentUsername = user.username, currentUserId = user.id) }
            }.onFailure { e -> AppLogger.e(TAG, "Load current user failed", e) }
            searchUsers(_state.value.query)
        }
    }

    fun onIntent(intent: NewChatIntent) {
        when (intent) {
            is NewChatIntent.QueryChanged -> {
                _state.update { it.copy(query = intent.query) }
                viewModelScope.launch { searchUsers(intent.query) }
            }
            is NewChatIntent.ContactsLoaded -> {
                _state.update { it.copy(contacts = intent.contacts) }
                loadSuggestedContacts()
            }
            is NewChatIntent.ContactsPermissionDenied ->
                _state.update { it.copy(contactsPermissionDenied = true) }
            is NewChatIntent.UserAction -> handleUserAction(intent.otherUser)
            is NewChatIntent.BlockUser -> handleBlockUser(intent.otherUser)
            is NewChatIntent.UnblockUser -> handleUnblockUser(intent.otherUser)
            is NewChatIntent.UserScannedByQr -> handleQrScan(intent.userId)
            is NewChatIntent.SuggestedContactsLoaded ->
                _state.update { it.copy(suggestedContacts = intent.users, isLoadingSuggested = false) }
            is NewChatIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private suspend fun searchUsers(query: String) {
        _state.update { it.copy(isLoadingUsers = true, userRelationships = emptyMap()) }
        catchResult { searchUsersUseCase(query) }
            .onSuccess { users ->
                val selfId = _state.value.currentUserId
                val filtered = users.filter { it.id != selfId }
                _state.update { it.copy(appUsers = filtered) }
                loadRelationships(filtered)
            }
            .onFailure { e ->
                AppLogger.e(TAG, "User search failed", e)
                _state.update { it.copy(error = e.message) }
            }
        _state.update { it.copy(isLoadingUsers = false) }
    }

    private fun loadRelationships(users: List<UserBO>) {
        val currentUserId = _state.value.currentUserId.takeIf { it.isNotBlank() } ?: return
        for (user in users) {
            if (_state.value.userRelationships.containsKey(user.id)) continue
            viewModelScope.launch {
                val rel = catchResult {
                    sendInvitationUseCase.checkRelationship(currentUserId, user.id)
                }.getOrDefault(UserRelationship.NONE)
                _state.update { it.copy(userRelationships = it.userRelationships + (user.id to rel)) }
            }
        }
    }

    private fun handleUserAction(otherUser: UserBO) {
        val currentRel = _state.value.userRelationships[otherUser.id]

        if (currentRel == UserRelationship.PENDING_RECEIVED) {
            viewModelScope.launch { _effect.send(NewChatEffect.NavigateToInvitations) }
            return
        }
        if (currentRel == UserRelationship.PENDING_SENT) {
            viewModelScope.launch { _effect.send(NewChatEffect.ShowMessage("Invitación enviada · Pendiente de respuesta de @${otherUser.username}")) }
            return
        }

        _state.update { it.copy(pendingUserIds = it.pendingUserIds + otherUser.id) }
        viewModelScope.launch {
            when (val result = sendInvitationUseCase(otherUser)) {
                is SendInvitationResult.Sent -> {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.PENDING_SENT))
                    }
                    _effect.send(NewChatEffect.ShowMessage("¡Invitación enviada a @${otherUser.username}!"))
                }
                is SendInvitationResult.AlreadySent -> {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.PENDING_SENT))
                    }
                    _effect.send(NewChatEffect.ShowMessage("Invitación enviada · Pendiente de respuesta de @${otherUser.username}"))
                }
                is SendInvitationResult.PendingReceived -> {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.PENDING_RECEIVED))
                    }
                    _effect.send(NewChatEffect.NavigateToInvitations)
                }
                is SendInvitationResult.NavigateToChat -> {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.CONNECTED))
                    }
                    _effect.send(NewChatEffect.NavigateToChat(result.conversationId, result.name))
                }
                is SendInvitationResult.Blocked -> {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.BLOCKED))
                    }
                    _effect.send(NewChatEffect.ShowMessage("No puedes enviar una invitación a @${otherUser.username}"))
                }
                is SendInvitationResult.Failure -> {
                    AppLogger.e(TAG, "User action failed: ${result.message}")
                    _state.update { it.copy(error = result.message) }
                }
            }
            _state.update { it.copy(pendingUserIds = it.pendingUserIds - otherUser.id) }
        }
    }

    private fun handleBlockUser(otherUser: UserBO) {
        _state.update { it.copy(pendingUserIds = it.pendingUserIds + otherUser.id) }
        viewModelScope.launch {
            blockUserUseCase.block(otherUser.id)
                .onSuccess {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.BLOCKED))
                    }
                    _effect.send(NewChatEffect.ShowMessage("@${otherUser.username} bloqueado"))
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Block user failed", e)
                    _state.update { it.copy(error = e.message) }
                }
            _state.update { it.copy(pendingUserIds = it.pendingUserIds - otherUser.id) }
        }
    }

    private fun handleUnblockUser(otherUser: UserBO) {
        _state.update { it.copy(pendingUserIds = it.pendingUserIds + otherUser.id) }
        viewModelScope.launch {
            blockUserUseCase.unblock(otherUser.id)
                .onSuccess {
                    _state.update {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.NONE))
                    }
                    _effect.send(NewChatEffect.ShowMessage("@${otherUser.username} desbloqueado"))
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Unblock user failed", e)
                    _state.update { it.copy(error = e.message) }
                }
            _state.update { it.copy(pendingUserIds = it.pendingUserIds - otherUser.id) }
        }
    }

    private fun loadSuggestedContacts() {
        _state.update { it.copy(isLoadingSuggested = true) }
        viewModelScope.launch {
            val emails = withContext(Dispatchers.IO) { contactSyncManager.readContactEmails() }
            if (emails.isEmpty()) {
                _state.update { it.copy(isLoadingSuggested = false) }
                return@launch
            }
            catchResult { userRepository.searchUsersByEmails(emails) }
                .onSuccess { users ->
                    val selfId = _state.value.currentUserId
                    val filtered = users.filter { it.id != selfId }
                    _state.update { it.copy(suggestedContacts = filtered, isLoadingSuggested = false) }
                    loadRelationships(filtered)
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Suggested contacts load failed", e)
                    _state.update { it.copy(isLoadingSuggested = false) }
                }
        }
    }

    private fun handleQrScan(userId: String) {
        val selfId = _state.value.currentUserId
        if (userId == selfId) {
            viewModelScope.launch { _effect.send(NewChatEffect.ShowMessage("Este es tu propio código QR")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoadingUsers = true) }
            catchResult { userRepository.getUserById(userId) }
                .onSuccess { user ->
                    if (user != null) {
                        _state.update { it.copy(appUsers = listOf(user), query = "") }
                        loadRelationships(listOf(user))
                    } else {
                        _effect.send(NewChatEffect.ShowMessage("Usuario no encontrado"))
                    }
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "QR scan lookup failed", e)
                    _effect.send(NewChatEffect.ShowMessage("No se pudo encontrar el usuario"))
                }
            _state.update { it.copy(isLoadingUsers = false) }
        }
    }

    companion object {
        private const val TAG = "NewChatViewModel"
    }
}
