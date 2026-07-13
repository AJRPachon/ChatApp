package com.ajrpachon.chatapp.ui.group
import com.ajrpachon.chatapp.utils.catchResult

import androidx.lifecycle.viewModelScope
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.utils.AppConstants
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.UpdateGroupUseCase
import com.ajrpachon.chatapp.ui.common.BaseViewModel
import kotlinx.coroutines.delay
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
    private val conversationRepository: ConversationRepository,
) : BaseViewModel<GroupInfoState, GroupInfoEffect>(GroupInfoState()) {

    init {
        viewModelScope.launch {
            conversationRepository.observeById(conversationId).collect { conv ->
                if (conv != null) {
                    updateState { it.copy(groupAvatarUrl = conv.groupAvatarUrl) }
                }
            }
        }
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId()
            updateState { it.copy(currentUserId = currentUserId) }
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
                val admins = members.filter { it.role == GroupRole.ADMIN }
                val adminCount = admins.size
                val lastAdminId = if (adminCount == 1) admins.first().userId else null
                updateState { currentState ->
                    currentState.copy(
                        members = members,
                        currentUserRole = currentRole,
                        isCurrentUserAdmin = isAdmin,
                        adminCount = adminCount,
                        lastAdminId = lastAdminId,
                    )
                }
            }
        }
    }

    fun setGroupHeader(name: String, description: String?, avatarUrl: String?) {
        updateState { it.copy(groupName = name, groupDescription = description ?: "", groupAvatarUrl = avatarUrl) }
    }

    fun onIntent(intent: GroupInfoIntent) {
        when (intent) {
            GroupInfoIntent.OpenEditDialog -> updateState { it.copy(showEditDialog = true) }
            GroupInfoIntent.CloseEditDialog -> updateState { it.copy(showEditDialog = false) }
            is GroupInfoIntent.NameChanged -> updateState { it.copy(groupName = intent.name) }
            is GroupInfoIntent.DescriptionChanged -> updateState { it.copy(groupDescription = intent.description) }
            GroupInfoIntent.SaveGroupInfo -> saveGroupInfo()
            is GroupInfoIntent.PickAvatar -> pickAvatar(intent.bytes, intent.mimeType)
            is GroupInfoIntent.RemoveMember -> removeMember(intent.userId)
            is GroupInfoIntent.PromoteMember -> changeRole(intent.userId, promote = true)
            is GroupInfoIntent.DemoteMember -> changeRole(intent.userId, promote = false)
            GroupInfoIntent.LeaveGroup -> leaveGroup()
            GroupInfoIntent.OpenAddMember -> updateState { it.copy(showAddMemberSheet = true) }
            GroupInfoIntent.CloseAddMember -> updateState { it.copy(showAddMemberSheet = false, addMemberQuery = "", addMemberResults = emptyList()) }
            is GroupInfoIntent.AddMemberQueryChanged -> searchForAdd(intent.query)
            is GroupInfoIntent.AddMember -> updateState { it.copy(pendingAddUser = intent.user, showHistoryDialog = true, showAddMemberSheet = false) }
            is GroupInfoIntent.ConfirmAddMember -> confirmAddMember(intent.canSeeHistory)
            GroupInfoIntent.DismissHistoryDialog -> updateState { it.copy(showHistoryDialog = false, pendingAddUser = null, showAddMemberSheet = true) }
            GroupInfoIntent.DismissError -> updateState { it.copy(error = null) }
            GroupInfoIntent.GenerateInviteLink -> {
                val code = java.util.UUID.randomUUID().toString().take(8).uppercase()
                val link = "${AppConstants.GROUP_INVITE_BASE_URL}?g=$conversationId&c=$code"
                updateState { it.copy(inviteLink = link, showInviteLinkSheet = true) }
            }
            GroupInfoIntent.DismissInviteLinkSheet -> updateState { it.copy(showInviteLinkSheet = false) }
            GroupInfoIntent.ShareInviteLink -> {
                val url = state.value.inviteLink ?: return
                sendEffect(GroupInfoEffect.ShareInviteLink(url))
                updateState { it.copy(showInviteLinkSheet = false) }
            }
        }
    }

    private fun saveGroupInfo() {
        val currentState = state.value
        viewModelScope.launch {
            updateState { it.copy(isSaving = true, showEditDialog = false) }
            updateGroupUseCase(conversationId, name = currentState.groupName, description = currentState.groupDescription)
                .onFailure { e -> updateState { it.copy(error = e.message) } }
                .onSuccess { sendEffect(GroupInfoEffect.ShowMessage("Grupo actualizado")) }
            updateState { it.copy(isSaving = false) }
        }
    }

    private fun pickAvatar(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            updateState { it.copy(isSaving = true) }
            catchResult {
                val url = groupRepository.uploadGroupAvatar(conversationId, bytes)
                updateGroupUseCase(conversationId, avatarUrl = url)
                updateState { it.copy(groupAvatarUrl = url) }
                sendEffect(GroupInfoEffect.ShowMessage("Foto actualizada"))
            }.onFailure { e -> updateState { it.copy(error = e.message) } }
            updateState { it.copy(isSaving = false) }
        }
    }

    private fun removeMember(userId: String) {
        viewModelScope.launch {
            catchResult { groupRepository.removeMember(conversationId, userId) }
                .onFailure { e -> updateState { it.copy(error = e.message) } }
        }
    }

    private fun changeRole(userId: String, promote: Boolean) {
        if (!promote) {
            val admins = state.value.members.filter { it.role == GroupRole.ADMIN }
            if (admins.size == 1 && admins[0].userId == userId) {
                updateState { it.copy(error = "No puedes quitar el rol al único administrador del grupo") }
                return
            }
        }
        viewModelScope.launch {
            catchResult {
                if (promote) groupRepository.promoteMember(conversationId, userId)
                else groupRepository.demoteMember(conversationId, userId)
            }.onFailure { e -> updateState { it.copy(error = e.message) } }
        }
    }

    private fun leaveGroup() {
        val userId = state.value.currentUserId ?: return
        viewModelScope.launch {
            leaveGroupUseCase(conversationId, userId)
                .onSuccess { sendEffect(GroupInfoEffect.NavigateBack) }
                .onFailure { e -> updateState { it.copy(error = e.message) } }
        }
    }

    private fun searchForAdd(q: String) {
        updateState { it.copy(addMemberQuery = q) }
        if (q.isBlank()) { updateState { it.copy(addMemberResults = emptyList()) }; return }
        viewModelScope.launch {
            val existing = state.value.members.map { it.userId }.toSet()
            val results = catchResult { searchUsersUseCase(q) }.getOrDefault(emptyList())
            updateState { it.copy(addMemberResults = results.filter { candidate -> candidate.id !in existing }) }
        }
    }

    private fun confirmAddMember(canSeeHistory: Boolean) {
        val user = state.value.pendingAddUser ?: return
        updateState { it.copy(showHistoryDialog = false, pendingAddUser = null, addMemberQuery = "", addMemberResults = emptyList()) }
        viewModelScope.launch {
            catchResult { groupRepository.addMember(conversationId, user.id, canSeeHistory) }
                .onSuccess { sendEffect(GroupInfoEffect.ShowMessage("${user.displayName} añadido al grupo")) }
                .onFailure { e -> updateState { it.copy(error = e.message) } }
        }
    }
}
