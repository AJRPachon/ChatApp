package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSentInvitationsUseCaseTest {

    private val repository = mockk<InvitationRepository>()
    private val useCase = GetSentInvitationsUseCase(repository)

    private val me = UserBO("me", "me@test.com", "me", "Me", null, Instant.fromEpochMilliseconds(0))
    private val other = UserBO("u2", "u2@test.com", "u2", "Ana", null, Instant.fromEpochMilliseconds(0))

    @Test
    fun `it returns the invitations the user sent`() = runTest {
        val sent = InvitationBO(
            id = "inv1",
            sender = me,
            receiverId = other.id,
            status = InvitationStatus.PENDING,
            createdAt = Instant.fromEpochMilliseconds(0),
            receiver = other,
        )
        coEvery { repository.getSentInvitations("me") } returns Result.success(listOf(sent))

        assertEquals(listOf(sent), useCase("me").getOrThrow())
    }

    @Test
    fun `a repository failure is returned, not thrown`() = runTest {
        val failure = IllegalStateException("offline")
        coEvery { repository.getSentInvitations("me") } returns Result.failure(failure)

        assertEquals(failure, useCase("me").exceptionOrNull())
    }
}
