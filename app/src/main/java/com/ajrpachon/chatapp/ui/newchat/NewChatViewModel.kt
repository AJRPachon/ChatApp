package com.ajrpachon.chatapp.ui.newchat
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.utils.ContactSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ajrpachon.chatapp.domain.usecase.BlockUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetDeviceContactsUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationResult
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import com.ajrpachon.chatapp.utils.AppLogger
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NewChatViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val sendInvitationUseCase: SendInvitationUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val userRepository: UserRepository,
    private val contactSyncManager: ContactSyncManager,
    private val getDeviceContactsUseCase: GetDeviceContactsUseCase,
) : BaseViewModel<NewChatState, NewChatEffect>(NewChatState()) {

    init {
        viewModelScope.launch {
            catchResult {
                val user = getCurrentUserUseCase().filterNotNull().first()
                updateState { it.copy(currentUsername = user.username, currentUserId = user.id) }
            }.onFailure { e -> AppLogger.e(TAG, "Load current user failed", e) }
            searchUsers(state.value.query)
        }
    }

    fun onIntent(intent: NewChatIntent) {
        when (intent) {
            is NewChatIntent.QueryChanged -> {
                updateState { it.copy(query = intent.query) }
                viewModelScope.launch { searchUsers(intent.query) }
            }
            is NewChatIntent.ContactsLoaded -> {
                updateState { it.copy(contacts = intent.contacts) }
                loadSuggestedContacts()
            }
            is NewChatIntent.ContactsPermissionDenied ->
                updateState { it.copy(contactsPermissionDenied = true) }
            is NewChatIntent.LoadContacts -> loadDeviceContacts()
            is NewChatIntent.UserAction -> handleUserAction(intent.otherUser)
            is NewChatIntent.BlockUser -> handleBlockUser(intent.otherUser)
            is NewChatIntent.UnblockUser -> handleUnblockUser(intent.otherUser)
            is NewChatIntent.UserScannedByQr -> handleQrScan(intent.userId)
            is NewChatIntent.SuggestedContactsLoaded ->
                updateState { it.copy(suggestedContacts = intent.users, isLoadingSuggested = false) }
            is NewChatIntent.DismissError -> updateState { it.copy(error = null) }
            is NewChatIntent.CopyInviteCode -> sendEffect(NewChatEffect.CopyToClipboard("@${intent.username}"))
            is NewChatIntent.ShareInviteText -> {
                val text = "¡Únete a ChatApp! Búscame como @${intent.username} y hablamos 💬"
                sendEffect(NewChatEffect.ShareText(text))
            }
            is NewChatIntent.InviteContact -> {
                val text = "¡Únete a ChatApp! Búscame como @${intent.username} y hablamos 💬"
                sendEffect(NewChatEffect.InviteContact(intent.phoneNumber, text))
            }
        }
    }

    private fun loadDeviceContacts() {
        viewModelScope.launch {
            catchResult {
                val contacts = withContext(Dispatchers.IO) { getDeviceContactsUseCase() }
                val phoneContacts = contacts.map { PhoneContact(it.name, it.phoneNumber) }
                updateState { it.copy(contacts = phoneContacts) }
                loadSuggestedContacts()
            }.onFailure { e -> AppLogger.e(TAG, "loadDeviceContacts failed", e) }
        }
    }

    private suspend fun searchUsers(query: String) {
        updateState { it.copy(isLoadingUsers = true, userRelationships = emptyMap()) }
        catchResult { searchUsersUseCase(query) }
            .onSuccess { users ->
                val selfId = state.value.currentUserId
                val filtered = users.filter { it.id != selfId }
                updateState { it.copy(appUsers = filtered) }
                loadRelationships(filtered)
            }
            .onFailure { e ->
                AppLogger.e(TAG, "User search failed", e)
                updateState { it.copy(error = e.message) }
            }
        updateState { it.copy(isLoadingUsers = false) }
    }

    private fun loadRelationships(users: List<UserBO>) {
        val currentUserId = state.value.currentUserId.takeIf { it.isNotBlank() } ?: return
        for (user in users) {
            if (state.value.userRelationships.containsKey(user.id)) continue
            viewModelScope.launch {
                val rel = catchResult {
                    sendInvitationUseCase.checkRelationship(currentUserId, user.id)
                }.getOrDefault(UserRelationship.NONE)
                updateState { it.copy(userRelationships = it.userRelationships + (user.id to rel)) }
            }
        }
    }

    private fun handleUserAction(otherUser: UserBO) {
        val currentRel = state.value.userRelationships[otherUser.id]

        if (currentRel == UserRelationship.PENDING_RECEIVED) {
            viewModelScope.launch { sendEffect(NewChatEffect.NavigateToInvitations) }
            return
        }
        if (currentRel == UserRelationship.PENDING_SENT) {
            viewModelScope.launch { sendEffect(NewChatEffect.ShowMessage("Invitación enviada · Pendiente de respuesta de @${otherUser.username}")) }
            return
        }

        updateState { it.copy(pendingUserIds = it.pendingUserIds + otherUser.id) }
        viewModelScope.launch {
            when (val result = sendInvitationUseCase(otherUser)) {
                is SendInvitationResult.Sent -> {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.PENDING_SENT))
                    }
                    sendEffect(NewChatEffect.ShowMessage("¡Invitación enviada a @${otherUser.username}!"))
                }
                is SendInvitationResult.AlreadySent -> {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.PENDING_SENT))
                    }
                    sendEffect(NewChatEffect.ShowMessage("Invitación enviada · Pendiente de respuesta de @${otherUser.username}"))
                }
                is SendInvitationResult.PendingReceived -> {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.PENDING_RECEIVED))
                    }
                    sendEffect(NewChatEffect.NavigateToInvitations)
                }
                is SendInvitationResult.NavigateToChat -> {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.CONNECTED))
                    }
                    sendEffect(NewChatEffect.NavigateToChat(result.conversationId, result.name))
                }
                is SendInvitationResult.Blocked -> {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.BLOCKED))
                    }
                    sendEffect(NewChatEffect.ShowMessage("No puedes enviar una invitación a @${otherUser.username}"))
                }
                is SendInvitationResult.Failure -> {
                    AppLogger.e(TAG, "User action failed: ${result.message}")
                    updateState { it.copy(error = result.message) }
                }
            }
            updateState { it.copy(pendingUserIds = it.pendingUserIds - otherUser.id) }
        }
    }

    private fun handleBlockUser(otherUser: UserBO) {
        updateState { it.copy(pendingUserIds = it.pendingUserIds + otherUser.id) }
        viewModelScope.launch {
            blockUserUseCase.block(otherUser.id)
                .onSuccess {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.BLOCKED))
                    }
                    sendEffect(NewChatEffect.ShowMessage("@${otherUser.username} bloqueado"))
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Block user failed", e)
                    updateState { it.copy(error = e.message) }
                }
            updateState { it.copy(pendingUserIds = it.pendingUserIds - otherUser.id) }
        }
    }

    private fun handleUnblockUser(otherUser: UserBO) {
        updateState { it.copy(pendingUserIds = it.pendingUserIds + otherUser.id) }
        viewModelScope.launch {
            blockUserUseCase.unblock(otherUser.id)
                .onSuccess {
                    updateState {
                        it.copy(userRelationships = it.userRelationships + (otherUser.id to UserRelationship.NONE))
                    }
                    sendEffect(NewChatEffect.ShowMessage("@${otherUser.username} desbloqueado"))
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Unblock user failed", e)
                    updateState { it.copy(error = e.message) }
                }
            updateState { it.copy(pendingUserIds = it.pendingUserIds - otherUser.id) }
        }
    }

    private fun loadSuggestedContacts() {
        updateState { it.copy(isLoadingSuggested = true) }
        viewModelScope.launch {
            val emails = withContext(Dispatchers.IO) { contactSyncManager.readContactEmails() }
            if (emails.isEmpty()) {
                updateState { it.copy(isLoadingSuggested = false) }
                return@launch
            }
            catchResult { userRepository.searchUsersByEmails(emails) }
                .onSuccess { users ->
                    val selfId = state.value.currentUserId
                    val filtered = users.filter { it.id != selfId }
                    updateState { it.copy(suggestedContacts = filtered, isLoadingSuggested = false) }
                    loadRelationships(filtered)
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "Suggested contacts load failed", e)
                    updateState { it.copy(isLoadingSuggested = false) }
                }
        }
    }

    private fun handleQrScan(userId: String) {
        val selfId = state.value.currentUserId
        if (userId == selfId) {
            viewModelScope.launch { sendEffect(NewChatEffect.ShowMessage("Este es tu propio código QR")) }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoadingUsers = true) }
            catchResult { userRepository.getUserById(userId) }
                .onSuccess { user ->
                    if (user != null) {
                        updateState { it.copy(appUsers = listOf(user), query = "") }
                        loadRelationships(listOf(user))
                    } else {
                        sendEffect(NewChatEffect.ShowMessage("Usuario no encontrado"))
                    }
                }
                .onFailure { e ->
                    AppLogger.e(TAG, "QR scan lookup failed", e)
                    sendEffect(NewChatEffect.ShowMessage("No se pudo encontrar el usuario"))
                }
            updateState { it.copy(isLoadingUsers = false) }
        }
    }

    companion object {
        private const val TAG = "NewChatViewModel"
    }
}
