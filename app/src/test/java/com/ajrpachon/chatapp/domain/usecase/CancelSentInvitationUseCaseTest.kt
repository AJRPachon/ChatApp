package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelSentInvitationUseCaseTest {

    private val repository = mockk<InvitationRepository>()
    private val useCase = CancelSentInvitationUseCase(repository)

    @Test
    fun `cancelling asks the repository to cancel that invitation`() = runTest {
        coEvery { repository.cancelSentInvitation("inv1") } returns Result.success(Unit)

        val result = useCase("inv1")

        assertTrue(result.isSuccess)
        coVerify { repository.cancelSentInvitation("inv1") }
    }

    @Test
    fun `a repository failure is returned, not thrown`() = runTest {
        val failure = IllegalStateException("not found")
        coEvery { repository.cancelSentInvitation("inv1") } returns Result.failure(failure)

        assertEquals(failure, useCase("inv1").exceptionOrNull())
    }
}
