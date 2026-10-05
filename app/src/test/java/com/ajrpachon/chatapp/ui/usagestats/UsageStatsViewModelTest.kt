package com.ajrpachon.chatapp.ui.usagestats

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageStatsRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant as JavaInstant
import java.time.ZoneId

class UsageStatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val stats = mockk<MessageStatsRepository>()
    private val conversations = mockk<ConversationRepository>()
    private val getCurrentUser = mockk<GetCurrentUserUseCase>()

    @Before
    fun setUp() {
        every { getCurrentUser() } returns MutableStateFlow(
            UserBO("me", "me@test.com", "me", "Me", null, Instant.fromEpochMilliseconds(0)),
        )
        coEvery { stats.countSent("me") } returns 10
        coEvery { stats.countReceived("me") } returns 20
        coEvery { stats.countCalls() } returns 3
        coEvery { stats.sumCallDurationSeconds() } returns 125
        coEvery { stats.countImages() } returns 4
        coEvery { stats.countAudio() } returns 5
        coEvery { stats.countVideos() } returns 6
        coEvery { stats.getMostActiveConversationId() } returns null
        coEvery { stats.countMessagesByDay(any()) } returns emptyList()
    }

    private fun buildViewModel() = UsageStatsViewModel(stats, conversations, getCurrentUser)

    private fun conversation(name: String) = ConversationBO(
        id = "c1",
        name = name,
        isGroup = false,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun `counters are loaded into the state and call time is shown in whole minutes`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()

            val state = vm.state.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(10, state.totalMessagesSent)
            assertEquals(20, state.totalMessagesReceived)
            assertEquals(3, state.totalCalls)
            assertEquals(2, state.totalCallMinutes)
            assertEquals(4, state.totalImages)
            assertEquals(5, state.totalAudio)
            assertEquals(6, state.totalVideos)
            assertEquals("", state.mostActiveConvName)
        }

    @Test
    fun `the most active conversation is shown by name`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { stats.getMostActiveConversationId() } returns "c1"
        coEvery { conversations.getById("c1") } returns conversation("Equipo Android")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("Equipo Android", vm.state.value.mostActiveConvName)
    }

    @Test
    fun `a most active conversation that no longer exists leaves the name empty`() =
        runTest(mainDispatcherRule.scheduler) {
            coEvery { stats.getMostActiveConversationId() } returns "gone"
            coEvery { conversations.getById("gone") } returns null

            val vm = buildViewModel()
            advanceUntilIdle()

            assertEquals("", vm.state.value.mostActiveConvName)
        }

    @Test
    fun `messages per day always covers the last seven days, filling gaps with zero`() =
        runTest(mainDispatcherRule.scheduler) {
            val today = JavaInstant.now().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            coEvery { stats.countMessagesByDay(any()) } returns listOf(today to 9, (today - 3) to 2)

            val vm = buildViewModel()
            advanceUntilIdle()

            val counts = vm.state.value.messagesPerDay.map { it.second }
            assertEquals(7, counts.size)
            assertEquals(9, counts.last())
            assertEquals(2, counts[3])
            assertEquals(11, counts.sum())
        }

    @Test
    fun `a failure while loading ends loading and sets an error`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { stats.countSent("me") } throws IllegalStateException("db closed")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals("db closed", vm.state.value.error)
    }

    @Test
    fun `reload queries the repository again and clears a previous error`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { stats.countSent("me") } throws IllegalStateException("db closed")
        val vm = buildViewModel()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        coEvery { stats.countSent("me") } returns 11

        vm.onIntent(UsageStatsIntent.Reload)
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertEquals(11, vm.state.value.totalMessagesSent)
        coVerify(exactly = 2) { stats.countSent("me") }
    }
}
