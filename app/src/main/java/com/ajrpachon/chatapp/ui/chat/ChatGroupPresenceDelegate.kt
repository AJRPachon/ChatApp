package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "ChatGroupPresenceDelegate"
private const val MEMBERSHIP_POLL_INTERVAL_MS = 3_000L

/**
 * Handles group-conversation presence: polling membership sync, observing the live member list
 * (exposed as [groupMembers], read by ChatViewModel's `@mention` suggestion matching) and each
 * member's online status (for [ChatGroupPresenceUiState]'s `onlineMemberCount`/`memberCount`),
 * and reacting when the current user joins or leaves the group.
 *
 * Row 9 of docs/chat-viewmodel-decomposition.md — deliberately last and separately designed
 * (unlike delegates 1-8c). Two things keep this from being a mechanical move:
 *
 * - Unlike the intent-driven delegates, nothing dispatches this from `onIntent` — [start] is
 *   called once from ChatViewModel's `init`, only for group conversations, mirroring the
 *   `if (isGroup) { ... }` block it replaces.
 * - Joining/leaving the group needs to sync or clear local messages and restart the remote-sync
 *   subscription, which read/write `_historyVisibleFrom` and `startRemoteSync` — both of which
 *   back the `messages` Flow ChatScreen collects, so they must stay owned by ChatViewModel
 *   rather than duplicated here. [onMembershipChanged] is the narrow hook for that.
 */
class ChatGroupPresenceDelegate(
    private val conversationId: String,
    private val groupRepository: GroupRepository,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val userRepository: UserRepository,
    private val scope: CoroutineScope,
    private val context: ChatDelegateContext,
    private val onMembershipChanged: suspend (isMember: Boolean) -> Unit,
) {
    private val updateState get() = context.updateState

    /** Live group member list, kept for `@mention` suggestion matching (see ChatViewModel). */
    var groupMembers: List<GroupMemberBO> = emptyList()
        private set

    private val memberOnlineStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private var memberObserveJob: Job? = null

    fun start(uid: String) {
        scope.launch {
            memberOnlineStatuses.collect { map ->
                updateState { it.copy(groupPresence = it.groupPresence.copy(onlineMemberCount = map.values.count { online -> online })) }
            }
        }
        scope.launch {
            catchResult { groupRepository.syncMembership(conversationId) }
            while (isActive) {
                delay(MEMBERSHIP_POLL_INTERVAL_MS)
                catchResult { groupRepository.syncMembership(conversationId) }
            }
        }
        scope.launch {
            var previousIsMember = true
            catchResult {
                getGroupMembersUseCase(conversationId).collect { members ->
                    groupMembers = members
                    val isMember = members.any { it.userId == uid }
                    updateState { it.copy(isCurrentUserMember = isMember, groupPresence = it.groupPresence.copy(memberCount = members.size)) }
                    memberObserveJob?.cancel()
                    memberObserveJob = launch {
                        memberOnlineStatuses.value = emptyMap()
                        for (member in members) {
                            launch {
                                catchResult {
                                    userRepository.observeUserById(member.userId).collect { user ->
                                        memberOnlineStatuses.value = memberOnlineStatuses.value + (member.userId to (user?.isOnline() == true))
                                    }
                                }
                            }
                        }
                    }
                    when {
                        isMember && !previousIsMember -> launch { onMembershipChanged(true) }
                        !isMember && previousIsMember -> launch { onMembershipChanged(false) }
                    }
                    previousIsMember = isMember
                }
            }.onFailure { e -> AppLogger.e(TAG, "getGroupMembers FAILED", e) }
        }
    }
}
