package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.data.local.dao.ConversationDao
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.ajrpachon.chatapp.util.sharedScheduler
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sendMessageUseCase = mockk<SendMessageUseCase>()
    private val messageRepository = mockk<MessageRepository>(relaxed = true)
    private val conversationDao = mockk<ConversationDao>()
    private val callRepository = mockk<CallRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val getGroupMembersUseCase = mockk<GetGroupMembersUseCase>()
    private val leaveGroupUseCase = mockk<LeaveGroupUseCase>(relaxed = true)
    private val groupRepository = mockk<GroupRepository>(relaxed = true)
    private val reactionRepository = mockk<ReactionRepository>(relaxed = true)

    // Start with current user as member — mirrors what the repository emits after initial fetch
    private val membersFlow = MutableStateFlow<List<GroupMemberBO>>(emptyList())
    // Populated in setUp() after member() helper is available

    private val testUser = UserBO(
        id = "user1",
        email = "user1@test.com",
        username = "user1",
        displayName = "User One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val groupConvDBO = ConversationDBO(
        id = "conv1",
        name = "Test Group",
        isGroup = true,
        createdBy = "creator",
        updatedAt = 0L,
    )

    private val dmConvDBO = ConversationDBO(
        id = "conv2",
        name = "DM",
        isGroup = false,
        createdBy = "user1",
        updatedAt = 0L,
        otherUserId = "user2",
    )

    @Before
    fun setUp() {
        membersFlow.value = listOf(member("user1")) // default: current user is a member
        every { getGroupMembersUseCase(any()) } returns membersFlow
        // Block the polling loop so it never spins: suspends at the first call so the while loop never runs.
        coEvery { groupRepository.syncMembership(any()) } coAnswers { awaitCancellation() }
        every { userRepository.getCurrentUserId() } returns "user1"
        coEvery { conversationDao.getById(any()) } returns groupConvDBO
        every { conversationDao.observeById(any()) } returns flowOf(groupConvDBO)
        coEvery { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Result.success(mockk<MessageBO>(relaxed = true))
    }

    private fun buildViewModel(conversationId: String = "conv1"): ChatViewModel =
        ChatViewModel(
            conversationId = conversationId,
            otherUserName = "Test Group",
            sendMessageUseCase = sendMessageUseCase,
            messageRepository = messageRepository,
            conversationDao = conversationDao,
            callRepository = callRepository,
            userRepository = userRepository,
            getGroupMembersUseCase = getGroupMembersUseCase,
            leaveGroupUseCase = leaveGroupUseCase,
            groupRepository = groupRepository,
            reactionRepository = reactionRepository,
        )

    // ── isCurrentUserMember ───────────────────────────────────────────────────

    @Test
    fun `isCurrentUserMember is true by default`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserMember)
    }

    @Test
    fun `isCurrentUserMember stays true when member list contains current user`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        membersFlow.value = listOf(member("user1"))
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserMember)
    }

    @Test
    fun `isCurrentUserMember becomes false when non-empty list does not contain current user`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        membersFlow.value = listOf(member("other-user"))
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentUserMember)
    }

    @Test
    fun `isCurrentUserMember becomes false when empty list received (repository guarantees definitive state)`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        membersFlow.value = emptyList()
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentUserMember)
    }

    @Test
    fun `isCurrentUserMember becomes false when expelled after being a member`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        membersFlow.value = listOf(member("user1"))
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserMember)

        membersFlow.value = emptyList()
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentUserMember)
    }

    @Test
    fun `isCurrentUserMember is true for DM conversations (non-group)`() = runTest(sharedScheduler) {
        coEvery { conversationDao.getById(any()) } returns dmConvDBO
        every { conversationDao.observeById(any()) } returns flowOf(dmConvDBO)
        val vm = buildViewModel("conv2")
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserMember)
    }

    @Test
    fun `isCurrentUserMember recovers to true when user re-appears in member list`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        membersFlow.value = listOf(member("user1"))
        advanceUntilIdle()
        membersFlow.value = listOf(member("other-user"))
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentUserMember)

        membersFlow.value = listOf(member("user1"), member("other-user"))
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentUserMember)
    }

    // ── Input & basic intents ─────────────────────────────────────────────────

    @Test
    fun `InputChanged intent updates inputText`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("hello"))
        assertEquals("hello", vm.state.value.inputText)
    }

    @Test
    fun `Send clears inputText and calls sendMessageUseCase`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("Hi!"))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        assertEquals("", vm.state.value.inputText)
        coVerify { sendMessageUseCase("conv1", "user1", "Hi!", any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Send does nothing when input is blank`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("   "))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        coVerify(exactly = 0) { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Send sets error when sendMessageUseCase fails`() = runTest(sharedScheduler) {
        coEvery { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("network error"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("test"))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        assertEquals("network error", vm.state.value.error)
    }

    @Test
    fun `DismissError clears error state`() = runTest(sharedScheduler) {
        coEvery { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("oops"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("msg"))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()
        assertEquals("oops", vm.state.value.error)

        vm.onIntent(ChatIntent.DismissError)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `CancelReply clears replyingTo`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.SetReply(mockk<MessageBO>(relaxed = true)))
        vm.onIntent(ChatIntent.CancelReply)
        assertNull(vm.state.value.replyingTo)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun member(userId: String) = GroupMemberBO(
        userId = userId,
        conversationId = "conv1",
        displayName = userId,
        username = userId,
        avatarUrl = null,
        role = GroupRole.MEMBER,
        joinedAt = Instant.fromEpochMilliseconds(0),
    )
}
