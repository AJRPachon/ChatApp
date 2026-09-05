package com.ajrpachon.chatapp.ui.conversations

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.LeaveGroupUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveConversationsUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.data.local.DraftRepository
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.domain.repository.NotificationSoundRepository
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.service.PresenceManager
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.utils.NetworkMonitor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConversationListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val observeConversationsUseCase = mockk<ObserveConversationsUseCase>()
    private val observeInvitationsUseCase = mockk<ObserveInvitationsUseCase>()
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val leaveGroupUseCase = mockk<LeaveGroupUseCase>(relaxed = true)
    private val fcmTokenRepository = mockk<FcmTokenRepository>(relaxed = true)
    private val presenceManager = mockk<PresenceManager>(relaxed = true)
    private val draftRepository = mockk<DraftRepository>(relaxed = true)
    private val notificationSoundRepository = mockk<NotificationSoundRepository>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)
    private val themeRepository = mockk<ThemeRepository>(relaxed = true)

    private val testUser = UserBO(
        id = "user1",
        email = "user@test.com",
        username = "user1",
        displayName = "User One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val userFlow = MutableStateFlow<UserBO?>(testUser)
    private val conversationsFlow = MutableStateFlow<List<ConversationBO>>(emptyList())
    private val invitationsFlow = MutableStateFlow<List<InvitationBO>>(emptyList())

    @Before
    fun setUp() {
        every { getCurrentUserUseCase() } returns userFlow
        every { observeConversationsUseCase(any()) } returns conversationsFlow
        every { observeInvitationsUseCase(any()) } returns invitationsFlow
        every { networkMonitor.isOnline } returns kotlinx.coroutines.flow.flowOf(true)
    }

    private fun buildViewModel() = ConversationListViewModel(
        getCurrentUserUseCase = getCurrentUserUseCase,
        observeConversationsUseCase = observeConversationsUseCase,
        observeInvitationsUseCase = observeInvitationsUseCase,
        conversationRepository = conversationRepository,
        leaveGroupUseCase = leaveGroupUseCase,
        fcmTokenRepository = fcmTokenRepository,
        presenceManager = presenceManager,
        draftRepository = draftRepository,
        notificationSoundRepository = notificationSoundRepository,
        networkMonitor = networkMonitor,
        themeRepository = themeRepository,
    )

    @Test
    fun `pendingInvitationsCount is 0 when no invitations`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.pendingInvitationsCount)
    }

    @Test
    fun `pendingInvitationsCount reflects number of pending invitations`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        invitationsFlow.value = listOf(invitation("inv1"), invitation("inv2"), invitation("inv3"))
        advanceUntilIdle()

        assertEquals(3, vm.state.value.pendingInvitationsCount)
    }

    @Test
    fun `pendingInvitationsCount decreases when invitation is accepted`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"), invitation("inv2"))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.pendingInvitationsCount)

        invitationsFlow.value = listOf(invitation("inv2"))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.pendingInvitationsCount)
    }

    @Test
    fun `pendingInvitationsCount resets to 0 when all invitations resolved`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.pendingInvitationsCount)

        invitationsFlow.value = emptyList()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.pendingInvitationsCount)
    }

    @Test
    fun `conversations state is updated from flow`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(emptyList<ConversationBO>(), vm.state.value.conversations)
        assertEquals(false, vm.state.value.isLoading)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun invitation(id: String) = InvitationBO(
        id = id,
        sender = testUser,
        receiverId = "user1",
        status = InvitationStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0),
    )
}
