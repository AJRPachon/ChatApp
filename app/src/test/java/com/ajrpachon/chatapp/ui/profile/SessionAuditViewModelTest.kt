package com.ajrpachon.chatapp.ui.profile

import com.ajrpachon.chatapp.domain.model.SessionBO
import com.ajrpachon.chatapp.domain.repository.AuthRepository
import com.ajrpachon.chatapp.domain.repository.SessionInfo
import com.ajrpachon.chatapp.domain.repository.SessionRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionAuditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val sessionRepository = mockk<SessionRepository>(relaxed = true)
    private val sessions = MutableStateFlow<List<SessionBO>>(emptyList())

    private val current = SessionBO("cur", "Pixel 9", 1L, 2L, isCurrent = true)
    private val other = SessionBO("oth", "Tablet", 3L, 4L, isCurrent = false)

    @Before
    fun setUp() {
        every { sessionRepository.observeAll() } returns sessions
        coEvery { authRepository.getCurrentSessionInfo() } returns null
    }

    private fun buildViewModel() = SessionAuditViewModel(authRepository, sessionRepository)

    @Test
    fun `sessions from the repository are mapped and loading ends`() = runTest(mainDispatcherRule.scheduler) {
        sessions.value = listOf(current, other)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("cur", "oth"), vm.state.value.sessions.map { it.id })
        assertTrue(vm.state.value.sessions.first().isCurrent)
    }

    @Test
    fun `the current session is recorded on start when there is a signed in session`() =
        runTest(mainDispatcherRule.scheduler) {
            coEvery { authRepository.getCurrentSessionInfo() } returns SessionInfo("user-1", "a@b.c")
            val saved = slot<SessionBO>()

            buildViewModel()
            advanceUntilIdle()

            coVerifyOrder {
                sessionRepository.updateCurrentLastActive(any())
                sessionRepository.upsert(capture(saved))
            }
            assertTrue(saved.captured.isCurrent)
            assertEquals(36, saved.captured.id.length)
        }

    @Test
    fun `nothing is recorded when there is no session`() = runTest(mainDispatcherRule.scheduler) {
        buildViewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { sessionRepository.upsert(any()) }
    }

    @Test
    fun `revoking another session deletes only that one and signals it`() = runTest(mainDispatcherRule.scheduler) {
        sessions.value = listOf(current, other)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SessionAuditIntent.RevokeSession("oth"))
        advanceUntilIdle()

        coVerify { sessionRepository.delete("oth") }
        coVerify(exactly = 0) { authRepository.signOut() }
        assertEquals(SessionAuditEffect.SessionRevoked, vm.effect.first())
    }

    @Test
    fun `revoking the current session signs out and clears every session`() = runTest(mainDispatcherRule.scheduler) {
        sessions.value = listOf(current, other)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SessionAuditIntent.RevokeSession("cur"))
        advanceUntilIdle()

        coVerify { authRepository.signOut() }
        coVerify { sessionRepository.deleteAll() }
        assertEquals(SessionAuditEffect.SessionRevoked, vm.effect.first())
    }

    @Test
    fun `a failure while revoking is reported as an error effect and in the state`() =
        runTest(mainDispatcherRule.scheduler) {
            sessions.value = listOf(current, other)
            coEvery { sessionRepository.delete("oth") } throws IllegalStateException("locked")
            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(SessionAuditIntent.RevokeSession("oth"))
            advanceUntilIdle()

            assertEquals("locked", vm.state.value.error)
            assertEquals(SessionAuditEffect.Error("locked"), vm.effect.first())
        }

    @Test
    fun `revoking all other sessions keeps the current one`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SessionAuditIntent.RevokeAllOtherSessions)
        advanceUntilIdle()

        coVerify { sessionRepository.deleteAllOthers() }
        coVerify(exactly = 0) { authRepository.signOut() }
        assertEquals(SessionAuditEffect.SessionRevoked, vm.effect.first())
    }

    @Test
    fun `a failure while revoking all others is reported`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { sessionRepository.deleteAllOthers() } throws IllegalStateException("locked")
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SessionAuditIntent.RevokeAllOtherSessions)
        advanceUntilIdle()

        assertEquals(SessionAuditEffect.Error("locked"), vm.effect.first())
        assertEquals("locked", vm.state.value.error)
    }

    @Test
    fun `refresh records the current session again`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { authRepository.getCurrentSessionInfo() } returns SessionInfo("user-1", null)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SessionAuditIntent.Refresh)
        advanceUntilIdle()

        coVerify(exactly = 2) { sessionRepository.upsert(any()) }
    }
}
