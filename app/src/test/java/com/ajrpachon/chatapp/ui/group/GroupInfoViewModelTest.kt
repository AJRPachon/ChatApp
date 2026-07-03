package com.ajrpachon.chatapp.ui.group

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.UpdateGroupUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    private val conversationDao = mockk<ConversationDao>(relaxed = true)

    private val conversationId = "conv1"

    private val convDBO = ConversationDBO(
        id = conversationId,
        name = "Test Group",
        isGroup = true,
        createdBy = "user1",
        updatedAt = 0L,
        groupAvatarUrl = "http://avatar.url",
    )

    private val membersFlow = MutableStateFlow<List<GroupMemberBO>>(emptyList())

    @Before
    fun setUp() {
        every { userRepository.getCurrentUserId() } returns "user1"
        every { conversationDao.observeById(any()) } returns flowOf(convDBO)
        every { getGroupMembersUseCase(any()) } returns membersFlow
        // Block the polling loop so it never spins
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
        conversationDao = conversationDao,
    )

    @Test
    fun `initial state loads currentUserId and groupAvatarUrl`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals("user1", vm.state.value.currentUserId)
        assertEquals("http://avatar.url", vm.state.value.groupAvatarUrl)
    }

    @Test
    fun `members are loaded from getGroupMembersUseCase`() = runTest(sharedScheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.ADMIN), member("user2", GroupRole.MEMBER))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.members.size)
    }

    @Test
    fun `current user as ADMIN sets isCurrentUserAdmin true`() = runTest(sharedScheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.ADMIN))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserAdmin)
        assertEquals(GroupRole.ADMIN, vm.state.value.currentUserRole)
    }

    @Test
    fun `current user as MEMBER sets isCurrentUserAdmin false`() = runTest(sharedScheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.MEMBER), member("user2", GroupRole.ADMIN))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentUserAdmin)
        assertEquals(GroupRole.MEMBER, vm.state.value.currentUserRole)
    }

    @Test
    fun `RemoveMember calls groupRepository removeMember`() = runTest(sharedScheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.ADMIN), member("user2", GroupRole.MEMBER))
        coEvery { groupRepository.removeMember(conversationId, "user2") } returns Unit

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.RemoveMember("user2"))
        advanceUntilIdle()
        // No error means success
        assertNull(vm.state.value.error)
    }

    @Test
    fun `RemoveMember failure sets error state`() = runTest(sharedScheduler) {
        membersFlow.value = listOf(member("user1", GroupRole.ADMIN), member("user2", GroupRole.MEMBER))
        coEvery { groupRepository.removeMember(any(), any()) } throws RuntimeException("permission denied")

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.RemoveMember("user2"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals("permission denied", vm.state.value.error)
    }

    @Test
    fun `SaveGroupInfo success emits ShowMessage effect`() = runTest(sharedScheduler) {
        coEvery { updateGroupUseCase(any(), name = any(), description = any()) } returns Result.success(Unit)

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.NameChanged("New Name"))
        vm.onIntent(GroupInfoIntent.SaveGroupInfo)
        advanceUntilIdle()

        val effect = vm.effect.first()
        assertTrue(effect is GroupInfoEffect.ShowMessage)
    }

    @Test
    fun `SaveGroupInfo failure sets error state`() = runTest(sharedScheduler) {
        coEvery { updateGroupUseCase(any(), name = any(), description = any()) } returns Result.failure(RuntimeException("update failed"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.NameChanged("Bad Name"))
        vm.onIntent(GroupInfoIntent.SaveGroupInfo)
        advanceUntilIdle()

        assertEquals("update failed", vm.state.value.error)
    }

    @Test
    fun `LeaveGroup success emits NavigateBack effect`() = runTest(sharedScheduler) {
        coEvery { leaveGroupUseCase(conversationId, "user1") } returns Result.success(Unit)

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.LeaveGroup)
        advanceUntilIdle()

        val effect = vm.effect.first()
        assertTrue(effect is GroupInfoEffect.NavigateBack)
    }

    @Test
    fun `LeaveGroup failure sets error state`() = runTest(sharedScheduler) {
        coEvery { leaveGroupUseCase(any(), any()) } returns Result.failure(RuntimeException("cannot leave"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.LeaveGroup)
        advanceUntilIdle()

        assertEquals("cannot leave", vm.state.value.error)
    }

    @Test
    fun `DismissError clears error`() = runTest(sharedScheduler) {
        coEvery { leaveGroupUseCase(any(), any()) } returns Result.failure(RuntimeException("oops"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(GroupInfoIntent.LeaveGroup)
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        vm.onIntent(GroupInfoIntent.DismissError)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `members list updates reactively when flow emits new values`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.members.size)

        membersFlow.value = listOf(member("user1", GroupRole.ADMIN), member("user2", GroupRole.MEMBER))
        advanceUntilIdle()
        assertEquals(2, vm.state.value.members.size)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
