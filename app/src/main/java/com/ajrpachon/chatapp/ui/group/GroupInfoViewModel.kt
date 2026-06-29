package com.ajrpachon.chatapp.ui.group
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.UpdateGroupUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GroupInfoViewModel(
    private val conversationId: String,
    private val userRepository: UserRepository,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val updateGroupUseCase: UpdateGroupUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val groupRepository: GroupRepository,
    private val conversationDao: ConversationDao,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupInfoState())
    val state = _state.asStateFlow()

    private val _effect = Channel<GroupInfoEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            conversationDao.observeById(conversationId).collect { conv ->
                if (conv != null) {
                    _state.update { it.copy(groupAvatarUrl = conv.groupAvatarUrl) }
                }
            }
        }
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId()
            _state.update { it.copy(currentUserId = currentUserId) }
            // Polling in a child coroutine — cancels reliably when viewModelScope clears on onCleared.
            launch {
                catchResult { groupRepository.syncMembership(conversationId) }
                while (isActive) {
                    delay(3_000)
                    catchResult { groupRepository.syncMembership(conversationId) }
                }
            }
            getGroupMembersUseCase(conversationId).collect { members ->
                AppLogger.d("GroupInfoVM", "members updated: size=${members.size} ids=${members.map { it.userId }}")
                val currentRole = members.firstOrNull { it.userId == currentUserId }?.role ?: GroupRole.MEMBER
                val isAdmin = currentRole == GroupRole.ADMIN
                _state.update { currentState ->
                    currentState.copy(
                        members = members,
                        currentUserRole = currentRole,
                        isCurrentUserAdmin = isAdmin,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.d("GroupInfoVM", "onCleared conv=$conversationId")
    }

    fun setGroupHeader(name: String, description: String?, avatarUrl: String?) {
        _state.update { it.copy(groupName = name, groupDescription = description ?: "", groupAvatarUrl = avatarUrl) }
    }

    fun onIntent(intent: GroupInfoIntent) {
        when (intent) {
            GroupInfoIntent.OpenEditDialog -> _state.update { it.copy(showEditDialog = true) }
            GroupInfoIntent.CloseEditDialog -> _state.update { it.copy(showEditDialog = false) }
            is GroupInfoIntent.NameChanged -> _state.update { it.copy(groupName = intent.name) }
            is GroupInfoIntent.DescriptionChanged -> _state.update { it.copy(groupDescription = intent.description) }
            GroupInfoIntent.SaveGroupInfo -> saveGroupInfo()
            is GroupInfoIntent.PickAvatar -> pickAvatar(intent.bytes, intent.mimeType)
            is GroupInfoIntent.RemoveMember -> removeMember(intent.userId)
            is GroupInfoIntent.PromoteMember -> changeRole(intent.userId, promote = true)
            is GroupInfoIntent.DemoteMember -> changeRole(intent.userId, promote = false)
            GroupInfoIntent.LeaveGroup -> leaveGroup()
            GroupInfoIntent.OpenAddMember -> _state.update { it.copy(showAddMemberSheet = true) }
            GroupInfoIntent.CloseAddMember -> _state.update { it.copy(showAddMemberSheet = false, addMemberQuery = "", addMemberResults = emptyList()) }
            is GroupInfoIntent.AddMemberQueryChanged -> searchForAdd(intent.query)
            is GroupInfoIntent.AddMember -> _state.update { it.copy(pendingAddUser = intent.user, showHistoryDialog = true, showAddMemberSheet = false) }
            is GroupInfoIntent.ConfirmAddMember -> confirmAddMember(intent.canSeeHistory)
            GroupInfoIntent.DismissHistoryDialog -> _state.update { it.copy(showHistoryDialog = false, pendingAddUser = null, showAddMemberSheet = true) }
            GroupInfoIntent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun saveGroupInfo() {
        val currentState = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, showEditDialog = false) }
            updateGroupUseCase(conversationId, name = currentState.groupName, description = currentState.groupDescription)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
                .onSuccess { _effect.send(GroupInfoEffect.ShowMessage("Grupo actualizado")) }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun pickAvatar(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            catchResult {
                val url = groupRepository.uploadGroupAvatar(conversationId, bytes)
                updateGroupUseCase(conversationId, avatarUrl = url)
                _state.update { it.copy(groupAvatarUrl = url) }
                _effect.send(GroupInfoEffect.ShowMessage("Foto actualizada"))
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isSaving = false) }
        }
    }

    private fun removeMember(userId: String) {
        viewModelScope.launch {
            catchResult { groupRepository.removeMember(conversationId, userId) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun changeRole(userId: String, promote: Boolean) {
        if (!promote) {
            val admins = _state.value.members.filter { it.role == GroupRole.ADMIN }
            if (admins.size == 1 && admins[0].userId == userId) {
                _state.update { it.copy(error = "No puedes quitar el rol al único administrador del grupo") }
                return
            }
        }
        viewModelScope.launch {
            catchResult {
                if (promote) groupRepository.promoteMember(conversationId, userId)
                else groupRepository.demoteMember(conversationId, userId)
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun leaveGroup() {
        val userId = _state.value.currentUserId ?: return
        viewModelScope.launch {
            leaveGroupUseCase(conversationId, userId)
                .onSuccess { _effect.send(GroupInfoEffect.NavigateBack) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun searchForAdd(q: String) {
        _state.update { it.copy(addMemberQuery = q) }
        if (q.isBlank()) { _state.update { it.copy(addMemberResults = emptyList()) }; return }
        viewModelScope.launch {
            val existing = _state.value.members.map { it.userId }.toSet()
            val results = catchResult { searchUsersUseCase(q) }.getOrDefault(emptyList())
            _state.update { it.copy(addMemberResults = results.filter { candidate -> candidate.id !in existing }) }
        }
    }

    private fun confirmAddMember(canSeeHistory: Boolean) {
        val user = _state.value.pendingAddUser ?: return
        _state.update { it.copy(showHistoryDialog = false, pendingAddUser = null, addMemberQuery = "", addMemberResults = emptyList()) }
        viewModelScope.launch {
            catchResult { groupRepository.addMember(conversationId, user.id, canSeeHistory) }
                .onSuccess { _effect.send(GroupInfoEffect.ShowMessage("${user.displayName} añadido al grupo")) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
