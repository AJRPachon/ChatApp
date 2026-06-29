package com.ajrpachon.chatapp.ui.group

import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.UserBO

data class GroupInfoState(
    val members: List<GroupMemberBO> = emptyList(),
    val currentUserId: String? = null,
    val groupName: String = "",
    val groupDescription: String = "",
    val groupAvatarUrl: String? = null,
    /** Role of the currently authenticated user in this group. */
    val currentUserRole: GroupRole = GroupRole.MEMBER,
    val isCurrentUserAdmin: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showEditDialog: Boolean = false,
    val showAddMemberSheet: Boolean = false,
    val addMemberQuery: String = "",
    val addMemberResults: List<UserBO> = emptyList(),
    val pendingAddUser: UserBO? = null,
    val showHistoryDialog: Boolean = false,
)

sealed interface GroupInfoIntent {
    data object OpenEditDialog : GroupInfoIntent
    data object CloseEditDialog : GroupInfoIntent
    data class NameChanged(val name: String) : GroupInfoIntent
    data class DescriptionChanged(val description: String) : GroupInfoIntent
    data object SaveGroupInfo : GroupInfoIntent
    data class PickAvatar(val bytes: ByteArray, val mimeType: String) : GroupInfoIntent
    data class RemoveMember(val userId: String) : GroupInfoIntent
    data class PromoteMember(val userId: String) : GroupInfoIntent
    data class DemoteMember(val userId: String) : GroupInfoIntent
    data object LeaveGroup : GroupInfoIntent
    data object OpenAddMember : GroupInfoIntent
    data object CloseAddMember : GroupInfoIntent
    data class AddMemberQueryChanged(val query: String) : GroupInfoIntent
    data class AddMember(val user: UserBO) : GroupInfoIntent
    data class ConfirmAddMember(val canSeeHistory: Boolean) : GroupInfoIntent
    data object DismissHistoryDialog : GroupInfoIntent
    data object DismissError : GroupInfoIntent
}

sealed interface GroupInfoEffect {
    data object NavigateBack : GroupInfoEffect
    data class ShowMessage(val message: String) : GroupInfoEffect
}
