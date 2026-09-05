package com.ajrpachon.chatapp.ui.invitations

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.ObserveInvitationsUseCase
import com.ajrpachon.chatapp.domain.usecase.RespondInvitationUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InvitationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val observeInvitationsUseCase = mockk<ObserveInvitationsUseCase>()
    private val respondInvitationUseCase = mockk<RespondInvitationUseCase>()
    private val getOrCreateConversationUseCase = mockk<GetOrCreateConversationUseCase>()

    private val sender = UserBO(
        id = "sender1",
        email = "sender@test.com",
        username = "sender1",
        displayName = "Sender One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val currentUser = UserBO(
        id = "user1",
        email = "user@test.com",
        username = "user1",
        displayName = "User One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val fakeConversation = mockk<ConversationBO>(relaxed = true).also {
        every { it.id } returns "conv1"
        every { it.name } returns "Sender One"
    }

    private val userFlow = MutableStateFlow<UserBO?>(currentUser)
    private val invitationsFlow = MutableStateFlow<List<InvitationBO>>(emptyList())

    @Before
    fun setUp() {
        every { getCurrentUserUseCase() } returns userFlow
        every { observeInvitationsUseCase(any()) } returns invitationsFlow
    }

    private fun buildViewModel() = InvitationsViewModel(
        getCurrentUserUseCase = getCurrentUserUseCase,
        observeInvitationsUseCase = observeInvitationsUseCase,
        respondInvitationUseCase = respondInvitationUseCase,
        getOrCreateConversationUseCase = getOrCreateConversationUseCase,
    )

    @Test
    fun `initial state loads invitations from flow`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"), invitation("inv2"))
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.invitations.size)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test
    fun `Accept success emits NavigateToChat effect`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"))
        coEvery { respondInvitationUseCase.accept("inv1") } returns Result.success(Unit)
        coEvery { getOrCreateConversationUseCase("user1", "sender1") } returns fakeConversation

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(InvitationsIntent.Accept("inv1"))
        advanceUntilIdle()

        val effect = vm.effect.first()
        assertTrue(effect is InvitationsEffect.NavigateToChat)
        assertEquals("conv1", (effect as InvitationsEffect.NavigateToChat).conversationId)
    }

    @Test
    fun `Accept failure updates error state`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"))
        coEvery { respondInvitationUseCase.accept("inv1") } returns Result.failure(RuntimeException("network error"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(InvitationsIntent.Accept("inv1"))
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertEquals("network error", vm.state.value.error)
    }

    @Test
    fun `Reject success emits ShowMessage effect`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"))
        coEvery { respondInvitationUseCase.reject("inv1") } returns Result.success(Unit)

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(InvitationsIntent.Reject("inv1"))
        advanceUntilIdle()

        val effect = vm.effect.first()
        assertTrue(effect is InvitationsEffect.ShowMessage)
    }

    @Test
    fun `Reject failure updates error state`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"))
        coEvery { respondInvitationUseCase.reject("inv1") } returns Result.failure(RuntimeException("reject failed"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(InvitationsIntent.Reject("inv1"))
        advanceUntilIdle()

        assertEquals("reject failed", vm.state.value.error)
    }

    @Test
    fun `DismissError clears error`() = runTest(mainDispatcherRule.scheduler) {
        invitationsFlow.value = listOf(invitation("inv1"))
        coEvery { respondInvitationUseCase.accept("inv1") } returns Result.failure(RuntimeException("oops"))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(InvitationsIntent.Accept("inv1"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        vm.onIntent(InvitationsIntent.DismissError)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `invitations list updates when flow emits new value`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.invitations.size)

        invitationsFlow.value = listOf(invitation("inv1"), invitation("inv2"))
        advanceUntilIdle()
        assertEquals(2, vm.state.value.invitations.size)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun invitation(id: String) = InvitationBO(
        id = id,
        sender = sender,
        receiverId = currentUser.id,
        status = InvitationStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0),
    )
}
