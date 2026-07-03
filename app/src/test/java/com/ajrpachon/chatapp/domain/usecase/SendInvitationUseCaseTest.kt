package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendInvitationUseCaseTest {

    private val invitationRepository = mockk<InvitationRepository>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val userRepository = mockk<UserRepository>()
    private val useCase = SendInvitationUseCase(invitationRepository, conversationRepository, userRepository)

    private val otherUser = mockk<UserBO>(relaxed = true).also {
        coEvery { it.id } returns "other-user-id"
        coEvery { it.displayName } returns "Other User"
    }
    private val fakeConversation = mockk<ConversationBO>(relaxed = true).also {
        coEvery { it.id } returns "conv-123"
    }

    @Test
    fun `returns Failure when user is not authenticated`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns null

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.Failure)
        assertEquals("No autenticado", (result as SendInvitationResult.Failure).message)
    }

    @Test
    fun `returns Blocked when relationship is BLOCKED`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.BLOCKED

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.Blocked)
    }

    @Test
    fun `returns NavigateToChat when relationship is CONNECTED`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.CONNECTED
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user-id", "other-user-id") } returns fakeConversation

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.NavigateToChat)
        val navResult = result as SendInvitationResult.NavigateToChat
        assertEquals("conv-123", navResult.conversationId)
        assertEquals("Other User", navResult.name)
    }

    @Test
    fun `returns Failure when CONNECTED but conversation repo throws`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.CONNECTED
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user-id", "other-user-id") } throws RuntimeException("DB error")

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.Failure)
        assertEquals("DB error", (result as SendInvitationResult.Failure).message)
    }

    @Test
    fun `returns AlreadySent when relationship is PENDING_SENT`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.PENDING_SENT

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.AlreadySent)
    }

    @Test
    fun `auto-accepts and navigates when PENDING_RECEIVED and invitation found`() = runTest {
        val fakeInvitation = mockk<InvitationBO>(relaxed = true).also {
            coEvery { it.id } returns "inv-456"
        }
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.PENDING_RECEIVED
        coEvery { invitationRepository.getPendingReceivedInvitation("current-user-id", "other-user-id") } returns fakeInvitation
        coEvery { invitationRepository.acceptInvitation("inv-456") } returns Result.success(Unit)
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user-id", "other-user-id") } returns fakeConversation

        val result = useCase(otherUser)

        coVerify { invitationRepository.acceptInvitation("inv-456") }
        assertTrue(result is SendInvitationResult.NavigateToChat)
        assertEquals("conv-123", (result as SendInvitationResult.NavigateToChat).conversationId)
    }

    @Test
    fun `returns PendingReceived when PENDING_RECEIVED but no invitation found`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.PENDING_RECEIVED
        coEvery { invitationRepository.getPendingReceivedInvitation("current-user-id", "other-user-id") } returns null

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.PendingReceived)
    }

    @Test
    fun `returns Sent when relationship is NONE and invitation succeeds`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.NONE
        coEvery { invitationRepository.sendInvitation("current-user-id", "other-user-id") } returns Result.success(mockk<InvitationBO>(relaxed = true))

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.Sent)
    }

    @Test
    fun `returns Failure when relationship is NONE and invitation fails`() = runTest {
        coEvery { userRepository.getCurrentUserId() } returns "current-user-id"
        coEvery { invitationRepository.getRelationship("current-user-id", "other-user-id") } returns UserRelationship.NONE
        coEvery { invitationRepository.sendInvitation("current-user-id", "other-user-id") } returns Result.failure(RuntimeException("Server error"))

        val result = useCase(otherUser)

        assertTrue(result is SendInvitationResult.Failure)
        assertEquals("Server error", (result as SendInvitationResult.Failure).message)
    }
}
