package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockUserUseCaseTest {

    private val invitationRepository = mockk<InvitationRepository>()
    private val userRepository = mockk<UserRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val useCase = BlockUserUseCase(invitationRepository, userRepository, analyticsTracker)

    @Test
    fun `block returns failure when user is not authenticated`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns null

        val result = useCase.block("target-user-id")

        assertTrue(result.isFailure)
        assertEquals("No autenticado", result.exceptionOrNull()?.message)
    }

    @Test
    fun `unblock returns failure when user is not authenticated`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns null

        val result = useCase.unblock("target-user-id")

        assertTrue(result.isFailure)
        assertEquals("No autenticado", result.exceptionOrNull()?.message)
    }

    @Test
    fun `block returns success and calls blockUser on repository`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.blockUser("current-user-id", "target-user-id") } returns Result.success(Unit)

        val result = useCase.block("target-user-id")

        assertTrue(result.isSuccess)
        coVerify { invitationRepository.blockUser("current-user-id", "target-user-id") }
    }

    @Test
    fun `unblock returns success and calls unblockUser on repository`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.unblockUser("current-user-id", "target-user-id") } returns Result.success(Unit)

        val result = useCase.unblock("target-user-id")

        assertTrue(result.isSuccess)
        coVerify { invitationRepository.unblockUser("current-user-id", "target-user-id") }
    }

    @Test
    fun `block propagates repository failure`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.blockUser("current-user-id", "target-user-id") } returns Result.failure(RuntimeException("Block failed"))

        val result = useCase.block("target-user-id")

        assertTrue(result.isFailure)
        assertEquals("Block failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `unblock propagates repository failure`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.unblockUser("current-user-id", "target-user-id") } returns Result.failure(RuntimeException("Unblock failed"))

        val result = useCase.unblock("target-user-id")

        assertTrue(result.isFailure)
        assertEquals("Unblock failed", result.exceptionOrNull()?.message)
    }
}
