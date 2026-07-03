package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import com.ajrpachon.chatapp.data.remote.dto.InvitationDTO
import com.ajrpachon.chatapp.domain.model.InvitationStatus
import com.ajrpachon.chatapp.domain.model.UserBO
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class InvitationMapperTest {

    // ── InvitationDTO.toDBO ───────────────────────────────────────────────────

    @Test
    fun `toDBO maps all fields from DTO`() {
        val dto = fakeDto(
            id = "inv1",
            senderId = "sender1",
            receiverId = "receiver1",
            status = "pending",
            createdAt = "2024-03-15T12:00:00Z",
        )

        val dbo = dto.toDBO()

        assertEquals("inv1", dbo.id)
        assertEquals("sender1", dbo.senderId)
        assertEquals("receiver1", dbo.receiverId)
        assertEquals("pending", dbo.status)
        assertEquals(1710504000000L, dbo.createdAt)
    }

    @Test
    fun `toDBO maps senderUsername from nested sender when present`() {
        val dto = fakeDto(senderUsername = "alice_username", senderDisplayName = "Alice")

        val dbo = dto.toDBO()

        assertEquals("alice_username", dbo.senderUsername)
        assertEquals("Alice", dbo.senderDisplayName)
    }

    @Test
    fun `toDBO uses empty strings for sender fields when sender is null`() {
        val dto = fakeDto(senderUsername = null, senderDisplayName = null)

        val dbo = dto.toDBO()

        assertEquals("", dbo.senderUsername)
        assertEquals("", dbo.senderDisplayName)
    }

    // ── InvitationDBO.toBO ────────────────────────────────────────────────────

    @Test
    fun `toBO maps status pending string to InvitationStatus PENDING`() {
        val dbo = fakeDbo(status = "pending")
        val bo = dbo.toBO(fakeSender())
        assertEquals(InvitationStatus.PENDING, bo.status)
    }

    @Test
    fun `toBO maps status accepted string to InvitationStatus ACCEPTED`() {
        val dbo = fakeDbo(status = "accepted")
        val bo = dbo.toBO(fakeSender())
        assertEquals(InvitationStatus.ACCEPTED, bo.status)
    }

    @Test
    fun `toBO maps status rejected string to InvitationStatus REJECTED`() {
        val dbo = fakeDbo(status = "rejected")
        val bo = dbo.toBO(fakeSender())
        assertEquals(InvitationStatus.REJECTED, bo.status)
    }

    @Test
    fun `toBO maps unknown status string to InvitationStatus PENDING`() {
        val dbo = fakeDbo(status = "unknown_status")
        val bo = dbo.toBO(fakeSender())
        assertEquals(InvitationStatus.PENDING, bo.status)
    }

    @Test
    fun `toBO maps senderId and receiverId correctly`() {
        val dbo = fakeDbo(senderId = "sender1", receiverId = "receiver1")
        val sender = fakeSender(id = "sender1")
        val bo = dbo.toBO(sender)

        assertEquals("sender1", bo.sender.id)
        assertEquals("receiver1", bo.receiverId)
    }

    @Test
    fun `toBO maps createdAt timestamp correctly`() {
        val dbo = fakeDbo(createdAt = 1710504000000L)
        val bo = dbo.toBO(fakeSender())
        assertEquals(1710504000000L, bo.createdAt.toEpochMilliseconds())
    }

    @Test
    fun `toBO maps invitation id correctly`() {
        val dbo = fakeDbo(id = "inv-42")
        val bo = dbo.toBO(fakeSender())
        assertEquals("inv-42", bo.id)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeDto(
        id: String = "inv1",
        senderId: String = "sender1",
        receiverId: String = "receiver1",
        status: String = "pending",
        createdAt: String = "2024-01-01T00:00:00Z",
        senderUsername: String? = "sender_user",
        senderDisplayName: String? = "Sender Name",
    ): InvitationDTO {
        val senderDto = if (senderUsername != null || senderDisplayName != null) {
            com.ajrpachon.chatapp.data.remote.dto.UserDTO(
                id = senderId,
                username = senderUsername,
                displayName = senderDisplayName ?: "",
            )
        } else null
        return InvitationDTO(
            id = id,
            senderId = senderId,
            receiverId = receiverId,
            status = status,
            createdAt = createdAt,
            sender = senderDto,
        )
    }

    private fun fakeDbo(
        id: String = "inv1",
        senderId: String = "sender1",
        receiverId: String = "receiver1",
        status: String = "pending",
        createdAt: Long = 0L,
    ) = InvitationDBO(
        id = id,
        senderId = senderId,
        senderUsername = "sender_user",
        senderDisplayName = "Sender Name",
        receiverId = receiverId,
        status = status,
        createdAt = createdAt,
    )

    private fun fakeSender(id: String = "sender1") = UserBO(
        id = id,
        email = "$id@example.com",
        username = id,
        displayName = "Sender",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0L),
        lastSeen = null,
        showOnlineStatus = true,
    )
}
