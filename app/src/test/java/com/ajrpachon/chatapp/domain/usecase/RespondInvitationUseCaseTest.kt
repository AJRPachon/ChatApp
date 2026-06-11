package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RespondInvitationUseCaseTest {

    private val invitationRepository = mockk<InvitationRepository>()
    private val useCase = RespondInvitationUseCase(invitationRepository)

    @Test
    fun `accept delegates to repository`() = runTest {
        coEvery { invitationRepository.acceptInvitation("inv1") } returns Result.success(Unit)

        val result = useCase.accept("inv1")

        assertTrue(result.isSuccess)
        coVerify { invitationRepository.acceptInvitation("inv1") }
    }

    @Test
    fun `reject delegates to repository`() = runTest {
        coEvery { invitationRepository.rejectInvitation("inv1") } returns Result.success(Unit)

        val result = useCase.reject("inv1")

        assertTrue(result.isSuccess)
        coVerify { invitationRepository.rejectInvitation("inv1") }
    }

    @Test
    fun `accept returns failure when repository fails`() = runTest {
        coEvery { invitationRepository.acceptInvitation(any()) } returns Result.failure(RuntimeException("network error"))

        val result = useCase.accept("inv1")

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `reject returns failure when repository fails`() = runTest {
        coEvery { invitationRepository.rejectInvitation(any()) } returns Result.failure(RuntimeException("network error"))

        val result = useCase.reject("inv1")

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }
}
