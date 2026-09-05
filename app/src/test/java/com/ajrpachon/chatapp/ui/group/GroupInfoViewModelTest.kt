package com.ajrpachon.chatapp.ui.group

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.UpdateGroupUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GroupInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val getGroupMembersUseCase = mockk<GetGroupMembersUseCase>()
    private val updateGroupUseCase = mockk<UpdateGroupUseCase>(relaxed = true)
    private val leaveGroupUseCase = mockk<LeaveGroupUseCase>(relaxed = true)
    private val searchUsersUseCase = mockk<SearchUsersUseCase>(relaxed = true)
    private val groupRepository = mockk<GroupRepository>(relaxed = true)
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)

    private val conversationId = "conv1"

    private val convBO = ConversationBO(
        id = conversationId,
        name = "Test Group",
        isGroup = true,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0),
        groupAvatarUrl = "http://avatar.url",
    )

    private val membersFlow = MutableStateFlow<List<GroupMemberBO>>(emptyList())

    @Before
    fun setUp() {
        every { userRepository.getCurrentUserId() } returns "user1"
        every { conversationRepository.observeById(any()) } returns flowOf(convBO)
        every { getGroupMembersUseCase(any()) } returns membersFlow
        coEvery { groupRepository.syncMembership(any()) } coAnswers { awaitCancellation() }
    }

    private fun buildViewModel() = GroupInfoViewModel(
        conversationId = conversationId,
        userRepository = userRepository,
        getGroupMembersUseCase = getGroupMembersUseCase,
        updateGroupUseCase = updateGroupUseCase,
        leaveGroupUseCase = leaveGroupUseCase,
        searchUsersUseCase = searchUsersUseCase,
        groupRepository = groupRepository,
        conversationRepository = conversationRepository,
    )

    @Test
    fun `initial state loads currentUserId and groupAvatarUrl`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals("user1", vm.state.value.currentUserId)
        assertEquals("http://avatar.url", vm.state.value.groupAvatarUrl)
    }

    @Test
    fun `members are loaded from getGroupMembersUseCase`() = runTest(mainDispatcherRule.scheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.ADMIN), member("user2", GroupRole.MEMBER))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.members.size)
    }

    @Test
    fun `isCurrentUserAdmin is true when user has ADMIN role`() = runTest(mainDispatcherRule.scheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.ADMIN))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserAdmin)
    }

    @Test
    fun `isCurrentUserAdmin is false when user has MEMBER role`() = runTest(mainDispatcherRule.scheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.MEMBER))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentUserAdmin)
    }

    private fun member(userId: String, role: GroupRole = GroupRole.MEMBER) = GroupMemberBO(
        userId = userId,
        conversationId = conversationId,
        displayName = userId,
        username = userId,
        avatarUrl = null,
        role = role,
        joinedAt = Instant.fromEpochMilliseconds(0),
    )
}
