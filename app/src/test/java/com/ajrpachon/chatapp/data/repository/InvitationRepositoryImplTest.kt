package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.InvitationDTO
import com.ajrpachon.chatapp.data.remote.source.InvitationRemoteSource
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InvitationRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val invitationDao = mockk<InvitationDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val remoteSource = mockk<InvitationRemoteSource>(relaxed = true)

    private val repo = InvitationRepositoryImpl(invitationDao, userDao, remoteSource)

    // ── getRelationship ───────────────────────────────────────────────────────

    @Test
    fun `getRelationship returns BLOCKED when isBlocked is true`() = runTest {
        coEvery { remoteSource.isBlocked("me", "other") } returns true

        val result = repo.getRelationship("me", "other")

        assertEquals(UserRelationship.BLOCKED, result)
    }

    @Test
    fun `getRelationship returns CONNECTED when there is an accepted invitation`() = runTest {
        coEvery { remoteSource.isBlocked(any(), any()) } returns false
        coEvery { remoteSource.getRelationshipInvitations("me", "other") } returns listOf(
            fakeDto(senderId = "me", receiverId = "other", status = "accepted")
        )

        val result = repo.getRelationship("me", "other")

        assertEquals(UserRelationship.CONNECTED, result)
    }

    @Test
    fun `getRelationship returns PENDING_SENT when currentUser is sender of pending invitation`() = runTest {
        coEvery { remoteSource.isBlocked(any(), any()) } returns false
        coEvery { remoteSource.getRelationshipInvitations("me", "other") } returns listOf(
            fakeDto(senderId = "me", receiverId = "other", status = "pending")
        )

        val result = repo.getRelationship("me", "other")

        assertEquals(UserRelationship.PENDING_SENT, result)
    }

    @Test
    fun `getRelationship returns PENDING_RECEIVED when currentUser is receiver of pending invitation`() = runTest {
        coEvery { remoteSource.isBlocked(any(), any()) } returns false
        coEvery { remoteSource.getRelationshipInvitations("me", "other") } returns listOf(
            fakeDto(senderId = "other", receiverId = "me", status = "pending")
        )

        val result = repo.getRelationship("me", "other")

        assertEquals(UserRelationship.PENDING_RECEIVED, result)
    }

    @Test
    fun `getRelationship returns NONE when there are no invitations`() = runTest {
        coEvery { remoteSource.isBlocked(any(), any()) } returns false
        coEvery { remoteSource.getRelationshipInvitations(any(), any()) } returns emptyList()

        val result = repo.getRelationship("me", "other")

        assertEquals(UserRelationship.NONE, result)
    }

    @Test
    fun `getRelationship returns NONE when remote throws`() = runTest {
        coEvery { remoteSource.isBlocked(any(), any()) } throws RuntimeException("network error")
        coEvery { remoteSource.getRelationshipInvitations(any(), any()) } throws RuntimeException("network error")

        val result = repo.getRelationship("me", "other")

        // Both catchResult calls return defaults (false / emptyList), so result is NONE
        assertEquals(UserRelationship.NONE, result)
    }

    // ── sendInvitation ────────────────────────────────────────────────────────

    @Test
    fun `sendInvitation succeeds and upserts invitation to Room`() = runTest {
        val dto = fakeDto(senderId = "me", receiverId = "other", status = "pending")
        coEvery { remoteSource.sendInvitation("me", "other") } returns dto
        coEvery { userDao.getById("me") } returns fakeUserDbo("me")

        val result = repo.sendInvitation("me", "other")

        assertTrue(result.isSuccess)
        coVerify { invitationDao.upsert(any()) }
    }

    @Test
    fun `sendInvitation returns failure when remote throws`() = runTest {
        coEvery { remoteSource.sendInvitation(any(), any()) } throws RuntimeException("network error")

        val result = repo.sendInvitation("me", "other")

        assertTrue(result.isFailure)
    }

    // ── acceptInvitation ──────────────────────────────────────────────────────

    @Test
    fun `acceptInvitation calls remote updateStatus and updates Room`() = runTest {
        val result = repo.acceptInvitation("inv1")

        assertTrue(result.isSuccess)
        coVerify { remoteSource.updateStatus("inv1", "accepted") }
        coVerify { invitationDao.updateStatus("inv1", "accepted") }
    }

    @Test
    fun `acceptInvitation returns failure when remote throws`() = runTest {
        coEvery { remoteSource.updateStatus("inv1", "accepted") } throws RuntimeException("error")

        val result = repo.acceptInvitation("inv1")

        assertTrue(result.isFailure)
    }

    // ── rejectInvitation ──────────────────────────────────────────────────────

    @Test
    fun `rejectInvitation calls remote updateStatus and updates Room`() = runTest {
        val result = repo.rejectInvitation("inv1")

        assertTrue(result.isSuccess)
        coVerify { remoteSource.updateStatus("inv1", "rejected") }
        coVerify { invitationDao.updateStatus("inv1", "rejected") }
    }

    @Test
    fun `rejectInvitation returns failure when remote throws`() = runTest {
        coEvery { remoteSource.updateStatus("inv1", "rejected") } throws RuntimeException("error")

        val result = repo.rejectInvitation("inv1")

        assertTrue(result.isFailure)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeDto(
        id: String = "inv1",
        senderId: String = "me",
        receiverId: String = "other",
        status: String = "pending",
    ) = InvitationDTO(
        id = id,
        senderId = senderId,
        receiverId = receiverId,
        status = status,
        createdAt = "2024-01-01T00:00:00Z",
        sender = null,
    )

    private fun fakeUserDbo(id: String) = UserDBO(
        id = id,
        email = "$id@example.com",
        username = id,
        displayName = id,
        avatarUrl = null,
        createdAt = 0L,
    )
}
