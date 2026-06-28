package com.ajrpachon.chatapp.ui.status

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.StatusBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.StatusRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import io.mockk.coEvery
import io.mockk.coVerify
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

class StatusViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val statusRepository = mockk<StatusRepository>(relaxed = true)
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()

    private val statusesFlow = MutableStateFlow<List<StatusBO>>(emptyList())
    private val conversationsFlow = MutableStateFlow<List<ConversationBO>>(emptyList())

    private val testUser = UserBO(
        id = "me",
        email = "me@test.com",
        username = "me",
        displayName = "Me",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0L),
    )

    @Before
    fun setUp() {
        every { statusRepository.observeActiveStatuses() } returns statusesFlow
        every { conversationRepository.observeConversations("me") } returns conversationsFlow
        every { getCurrentUserUseCase() } returns flowOf(testUser)
        coEvery { statusRepository.syncStatuses(any()) } returns Unit
    }

    private fun buildVm() = StatusViewModel(statusRepository, conversationRepository, getCurrentUserUseCase)

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has empty statuses and isLoading false`() = runTest(sharedScheduler) {
        val vm = buildVm()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.statuses.isEmpty())
        assertFalse(state.isLoading)
    }

    // ── observeActiveStatuses reflects repository flow ────────────────────────

    @Test
    fun `statuses updated when repository emits new list`() = runTest(sharedScheduler) {
        val vm = buildVm()
        advanceUntilIdle()

        val status = fakeStatus("s1", "alice")
        statusesFlow.emit(listOf(status))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.statuses.size)
        assertEquals("s1", vm.state.value.statuses[0].id)
    }

    @Test
    fun `statuses cleared when repository emits empty list`() = runTest(sharedScheduler) {
        statusesFlow.value = listOf(fakeStatus("s1", "alice"))
        val vm = buildVm()
        advanceUntilIdle()

        statusesFlow.emit(emptyList())
        advanceUntilIdle()

        assertTrue(vm.state.value.statuses.isEmpty())
    }

    // ── compose dialog ────────────────────────────────────────────────────────

    @Test
    fun `OpenCompose sets showComposeDialog to true`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.OpenCompose)

        assertTrue(vm.state.value.showComposeDialog)
    }

    @Test
    fun `CloseCompose sets showComposeDialog to false`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.OpenCompose)
        vm.onIntent(StatusIntent.CloseCompose)

        assertFalse(vm.state.value.showComposeDialog)
    }

    @Test
    fun `TextChanged updates composeText`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.TextChanged("¿Qué está pasando?"))

        assertEquals("¿Qué está pasando?", vm.state.value.composeText)
    }

    @Test
    fun `ColorChanged updates selectedColor`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.ColorChanged(0xFF0000FF))

        assertEquals(0xFF0000FF, vm.state.value.selectedColor)
    }

    @Test
    fun `OpenCompose resets composeText and color`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.TextChanged("old text"))
        vm.onIntent(StatusIntent.ColorChanged(0xFF000000))
        vm.onIntent(StatusIntent.OpenCompose)

        assertEquals("", vm.state.value.composeText)
        assertEquals(0xFF1976D2, vm.state.value.selectedColor)
    }

    // ── PostTextStatus ────────────────────────────────────────────────────────

    @Test
    fun `PostTextStatus calls repository and closes dialog`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.OpenCompose)
        vm.onIntent(StatusIntent.TextChanged("Hello world"))
        vm.onIntent(StatusIntent.PostTextStatus)
        advanceUntilIdle()

        coVerify { statusRepository.postTextStatus("Hello world", any()) }
        assertFalse(vm.state.value.showComposeDialog)
    }

    @Test
    fun `PostTextStatus with blank text does nothing`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.OpenCompose)
        vm.onIntent(StatusIntent.TextChanged("   "))
        vm.onIntent(StatusIntent.PostTextStatus)
        advanceUntilIdle()

        coVerify(exactly = 0) { statusRepository.postTextStatus(any(), any()) }
    }

    @Test
    fun `PostTextStatus trims whitespace before posting`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.OpenCompose)
        vm.onIntent(StatusIntent.TextChanged("  Hola  "))
        vm.onIntent(StatusIntent.PostTextStatus)
        advanceUntilIdle()

        coVerify { statusRepository.postTextStatus("Hola", any()) }
    }

    // ── DeleteStatus ──────────────────────────────────────────────────────────

    @Test
    fun `DeleteStatus calls repository with correct id`() = runTest(sharedScheduler) {
        val vm = buildVm()
        vm.onIntent(StatusIntent.DeleteStatus("s42"))
        advanceUntilIdle()

        coVerify { statusRepository.deleteStatus("s42") }
    }

    // ── Refresh / sync ────────────────────────────────────────────────────────

    @Test
    fun `Refresh triggers syncStatuses on init`() = runTest(sharedScheduler) {
        buildVm()
        advanceUntilIdle()

        coVerify(atLeast = 1) { statusRepository.syncStatuses(any()) }
    }

    @Test
    fun `error from repository sets error in state`() = runTest(sharedScheduler) {
        coEvery { statusRepository.postTextStatus(any(), any()) } throws RuntimeException("error de red")
        val vm = buildVm()
        vm.onIntent(StatusIntent.OpenCompose)
        vm.onIntent(StatusIntent.TextChanged("test"))
        vm.onIntent(StatusIntent.PostTextStatus)
        advanceUntilIdle()

        assertEquals("error de red", vm.state.value.error)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeStatus(id: String, userId: String) = StatusBO(
        id = id,
        userId = userId,
        userName = userId,
        userAvatarUrl = null,
        text = "test",
        imageUrl = null,
        backgroundColor = 0xFF1976D2,
        createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
        expiresAt = Instant.fromEpochMilliseconds(System.currentTimeMillis() + 60_000L),
        isFromMe = false,
    )
}
