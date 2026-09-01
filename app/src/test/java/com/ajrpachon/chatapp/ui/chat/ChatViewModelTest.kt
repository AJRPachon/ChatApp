package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.PollBO
import com.ajrpachon.chatapp.domain.model.PollOptionBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.utils.LinkPreviewData
import com.ajrpachon.chatapp.domain.repository.CallRepository
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.ReactionRepository
import com.ajrpachon.chatapp.domain.repository.ScheduledMessageRepository
import com.ajrpachon.chatapp.domain.repository.TypingRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.repository.ChatThemeRepository
import com.ajrpachon.chatapp.domain.repository.AiAssistantRepository
import com.ajrpachon.chatapp.domain.repository.ContactRepository
import com.ajrpachon.chatapp.domain.repository.DraftRepository
import com.ajrpachon.chatapp.domain.repository.IncognitoRepository
import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.domain.repository.WallpaperRepository
import android.app.Application
import com.ajrpachon.chatapp.utils.AudioTranscriber
import com.ajrpachon.chatapp.utils.ClipboardProtection
import com.ajrpachon.chatapp.utils.LinkPreviewFetcher
import com.ajrpachon.chatapp.utils.NetworkMonitor
import com.ajrpachon.chatapp.utils.TranslationManager
import androidx.work.WorkManager
import com.ajrpachon.chatapp.domain.usecase.ExportConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.GetGroupMembersUseCase
import com.ajrpachon.chatapp.domain.usecase.GetUriMetadataUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ReadUriAsBytesUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val callRepository = mockk<CallRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val getGroupMembersUseCase = mockk<GetGroupMembersUseCase>()
    private val leaveGroupUseCase = mockk<LeaveGroupUseCase>(relaxed = true)
    private val groupRepository = mockk<GroupRepository>(relaxed = true)
    private val reactionRepository = mockk<ReactionRepository>(relaxed = true)
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val scheduledMessageRepository = mockk<ScheduledMessageRepository>(relaxed = true)
    private val typingRepository = mockk<TypingRepository>(relaxed = true)
    private val draftRepository = mockk<DraftRepository>(relaxed = true)
    private val translationManager = mockk<TranslationManager>(relaxed = true)
    private val audioTranscriber = mockk<AudioTranscriber>(relaxed = true)
    private val pollRepository = mockk<PollRepository>(relaxed = true)
    private val chatThemeRepository = mockk<ChatThemeRepository>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val incognitoRepository = mockk<IncognitoRepository>(relaxed = true)
    private val aiAssistantRepository = mockk<AiAssistantRepository>(relaxed = true)
    private val wallpaperRepository = mockk<WallpaperRepository>(relaxed = true)
    private val contactRepository = mockk<ContactRepository>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)
    private val sendInvitationUseCase = mockk<SendInvitationUseCase>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val clipboardProtection = mockk<ClipboardProtection>(relaxed = true)
    private val exportConversationUseCase = mockk<ExportConversationUseCase>(relaxed = true)
    private val linkPreviewFetcher = mockk<LinkPreviewFetcher>(relaxed = true)
    private val getUriMetadataUseCase = mockk<GetUriMetadataUseCase>(relaxed = true)
    private val readUriAsBytesUseCase = mockk<ReadUriAsBytesUseCase>(relaxed = true)

    private val membersFlow = MutableStateFlow<List<GroupMemberBO>>(emptyList())

    private val testUser = UserBO(
        id = "user1",
        email = "user1@test.com",
        username = "user1",
        displayName = "User One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val groupConvBO = ConversationBO(
        id = "conv1",
        name = "Test Group",
        isGroup = true,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private val dmConvBO = ConversationBO(
        id = "conv2",
        name = "DM",
        isGroup = false,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0),
        otherUserId = "user2",
    )

    @Before
    fun setUp() {
        membersFlow.value = listOf(member("user1"))
        every { getGroupMembersUseCase(any()) } returns membersFlow
        coEvery { groupRepository.syncMembership(any()) } coAnswers { awaitCancellation() }
        every { userRepository.getCurrentUserId() } returns "user1"
        coEvery { conversationRepository.getById(any()) } returns groupConvBO
        every { conversationRepository.observeById(any()) } returns flowOf(groupConvBO)
        every { scheduledMessageRepository.observeAll() } returns flowOf(emptyList())
        every { typingRepository.observeTypingNames(any(), any()) } returns flowOf(emptyList())
        every { draftRepository.getDraft(any()) } returns flowOf("")
        every { conversationRepository.observeConversations(any()) } returns flowOf(emptyList())
        every { networkMonitor.isOnline } returns flowOf(true)
        coEvery {
            sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(mockk<MessageBO>(relaxed = true))
    }

    private fun buildViewModel(conversationId: String = "conv1"): ChatViewModel =
        ChatViewModel(
            args = ChatArgs(conversationId = conversationId, otherUserName = "Test Group"),
            application = application,
            clipboardProtection = clipboardProtection,
            sendMessageUseCase = sendMessageUseCase,
            messageRepository = messageRepository,
            callRepository = callRepository,
            userRepository = userRepository,
            getGroupMembersUseCase = getGroupMembersUseCase,
            leaveGroupUseCase = leaveGroupUseCase,
            groupRepository = groupRepository,
            reactionRepository = reactionRepository,
            conversationRepository = conversationRepository,
            scheduledMessageRepository = scheduledMessageRepository,
            typingRepository = typingRepository,
            draftRepository = draftRepository,
            translationManager = translationManager,
            audioTranscriber = audioTranscriber,
            pollRepository = pollRepository,
            contactRepository = contactRepository,
            chatThemeRepository = chatThemeRepository,
            workManager = workManager,
            incognitoRepository = incognitoRepository,
            aiAssistantRepository = aiAssistantRepository,
            wallpaperRepository = wallpaperRepository,
            networkMonitor = networkMonitor,
            sendInvitationUseCase = sendInvitationUseCase,
            exportConversationUseCase = exportConversationUseCase,
            linkPreviewFetcher = linkPreviewFetcher,
            getUriMetadataUseCase = getUriMetadataUseCase,
            readUriAsBytesUseCase = readUriAsBytesUseCase,
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
        coEvery { conversationRepository.getById(any()) } returns dmConvBO
        every { conversationRepository.observeById(any()) } returns flowOf(dmConvBO)
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
        coVerify { sendMessageUseCase("conv1", "user1", "Hi!", any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Send does nothing when input is blank`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("   "))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        coVerify(exactly = 0) { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Send sets error when sendMessageUseCase fails`() = runTest(sharedScheduler) {
        coEvery { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("network error"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("test"))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()

        assertEquals("Sin conexion. El mensaje se enviara cuando vuelva la red.", vm.state.value.error)
    }

    @Test
    fun `DismissError clears error state`() = runTest(sharedScheduler) {
        coEvery { sendMessageUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("oops"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.InputChanged("msg"))
        vm.onIntent(ChatIntent.Send)
        advanceUntilIdle()
        assertEquals("Sin conexion. El mensaje se enviara cuando vuelva la red.", vm.state.value.error)

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

    // ── Multi-select ──────────────────────────────────────────────────────────

    @Test
    fun `ToggleMessageSelection adds messageId to selectedMessageIds`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        assertTrue("msg1" in vm.state.value.selectedMessageIds)
    }

    @Test
    fun `ToggleMessageSelection removes already-selected messageId`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        assertFalse("msg1" in vm.state.value.selectedMessageIds)
    }

    @Test
    fun `ToggleMessageSelection can select multiple messages independently`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg2"))
        val selected = vm.state.value.selectedMessageIds
        assertTrue("msg1" in selected)
        assertTrue("msg2" in selected)
    }

    @Test
    fun `ClearSelection empties selectedMessageIds`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg2"))
        vm.onIntent(ChatIntent.ClearSelection)
        assertTrue(vm.state.value.selectedMessageIds.isEmpty())
    }

    @Test
    fun `isMultiSelectActive is true when at least one message is selected`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.isMultiSelectActive)
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        assertTrue(vm.state.value.isMultiSelectActive)
    }

    @Test
    fun `DeleteSelectedMessages calls deleteMessage for each selected id and clears selection`() = runTest(sharedScheduler) {
        coEvery { messageRepository.deleteMessage(any()) } returns Result.success(Unit)
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg1"))
        vm.onIntent(ChatIntent.ToggleMessageSelection("msg2"))
        vm.onIntent(ChatIntent.DeleteSelectedMessages)
        advanceUntilIdle()
        coVerify { messageRepository.deleteMessage("msg1") }
        coVerify { messageRepository.deleteMessage("msg2") }
        assertTrue(vm.state.value.selectedMessageIds.isEmpty())
    }

    // ── Forward dialog ────────────────────────────────────────────────────────

    @Test
    fun `ShowForwardDialog sets showForwardDialog to true and stores message`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val msg = mockk<MessageBO>(relaxed = true)
        vm.onIntent(ChatIntent.ShowForwardDialog(msg))
        advanceUntilIdle()
        assertTrue(vm.state.value.showForwardDialog)
        assertEquals(msg, vm.state.value.forwardingMessage)
    }

    @Test
    fun `DismissForwardDialog resets showForwardDialog and forwardingMessage`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ShowForwardDialog(mockk(relaxed = true)))
        vm.onIntent(ChatIntent.DismissForwardDialog)
        assertFalse(vm.state.value.showForwardDialog)
        assertNull(vm.state.value.forwardingMessage)
    }

    // ── Polls ─────────────────────────────────────────────────────────────────

    @Test
    fun `ObservePoll populates pollUiStates from pollRepository flows`() = runTest(sharedScheduler) {
        val poll = PollBO(id = "poll1", conversationId = "conv1", question = "Q?", createdBy = "user1", createdAt = 0L)
        val options = listOf(PollOptionBO(id = "opt1", pollId = "poll1", text = "A", voteCount = 2))
        every { pollRepository.observePollById("poll1") } returns flowOf(poll)
        every { pollRepository.observeOptionsByPollId("poll1") } returns flowOf(options)
        every { pollRepository.observeVotes("poll1", "user1") } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ObservePoll("poll1"))
        advanceUntilIdle()

        val pollUiState = vm.state.value.pollUiStates["poll1"]
        assertEquals(poll, pollUiState?.poll)
        assertEquals(options, pollUiState?.options)
    }

    @Test
    fun `ObservePoll for the same pollId only subscribes once`() = runTest(sharedScheduler) {
        every { pollRepository.observePollById("poll1") } returns flowOf(null)
        every { pollRepository.observeOptionsByPollId("poll1") } returns flowOf(emptyList())
        every { pollRepository.observeVotes("poll1", "user1") } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.ObservePoll("poll1"))
        vm.onIntent(ChatIntent.ObservePoll("poll1"))
        advanceUntilIdle()

        verify(exactly = 1) { pollRepository.observePollById("poll1") }
    }

    // ── Link previews ─────────────────────────────────────────────────────────

    @Test
    fun `DetectedUrlChanged stores fetched preview in linkPreviews`() = runTest(sharedScheduler) {
        val preview = LinkPreviewData(title = "Title", description = null, imageUrl = null, url = "https://example.com")
        coEvery { linkPreviewFetcher.fetchLinkPreview("https://example.com") } returns preview

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ChatIntent.DetectedUrlChanged("https://example.com"))
        advanceUntilIdle()

        assertEquals(preview, vm.state.value.linkPreviews["https://example.com"])
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
