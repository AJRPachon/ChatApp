package com.ajrpachon.chatapp.domain.usecase

import app.cash.turbine.test
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveInvitationsUseCaseTest {

    private val invitationRepository = mockk<InvitationRepository>()
    private val useCase = ObserveInvitationsUseCase(invitationRepository)

    @Test
    fun `delegates to repository with given userId`() = runTest {
        val invitations = listOf(mockk<InvitationBO>(relaxed = true))
        every { invitationRepository.observePendingInvitations("user-1") } returns flowOf(invitations)

        useCase("user-1").test {
            assertEquals(invitations, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits empty list when repository has no pending invitations`() = runTest {
        every { invitationRepository.observePendingInvitations("user-1") } returns flowOf(emptyList())

        useCase("user-1").test {
            assertEquals(emptyList<InvitationBO>(), awaitItem())
            awaitComplete()
        }
    }
}
