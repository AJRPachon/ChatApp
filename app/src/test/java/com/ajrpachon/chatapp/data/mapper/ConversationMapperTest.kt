package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import com.ajrpachon.chatapp.data.remote.dto.ConversationDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMapperTest {

    // ── toDBO ────────────────────────────────────────────────────────────────

    @Test
    fun `toDBO uses resolvedName when provided`() {
        val dto = fakeDto(name = "Server Name")

        val dbo = dto.toDBO(resolvedName = "Resolved Name")

        assertEquals("Resolved Name", dbo.name)
    }

    @Test
    fun `toDBO falls back to existing name when resolvedName is null`() {
        val dto = fakeDto(name = "Server Name")
        val existing = fakeExisting(name = "Cached Name")

        val dbo = dto.toDBO(resolvedName = null, existing = existing)

        assertEquals("Cached Name", dbo.name)
    }

    @Test
    fun `toDBO falls back to DTO name when neither resolvedName nor existing are set`() {
        val dto = fakeDto(name = "Server Name")

        val dbo = dto.toDBO(resolvedName = null, existing = null)

        assertEquals("Server Name", dbo.name)
    }

    @Test
    fun `toDBO defaults createdBy to empty string when DTO createdBy is null`() {
        val dto = fakeDto(createdBy = null)

        val dbo = dto.toDBO()

        assertEquals("", dbo.createdBy)
    }

    @Test
    fun `toDBO parses a valid updatedAt timestamp`() {
        val dto = fakeDto(updatedAt = "2024-01-01T00:00:00Z")

        val dbo = dto.toDBO()

        assertEquals(1704067200000L, dbo.updatedAt)
    }

    @Test
    fun `toDBO falls back to current time when updatedAt is malformed`() {
        val dto = fakeDto(updatedAt = "not-a-date")
        val before = System.currentTimeMillis()

        val dbo = dto.toDBO()

        assertTrue(dbo.updatedAt >= before)
    }

    @Test
    fun `toDBO preserves existing archive, unread, mute and disappearing state`() {
        val dto = fakeDto()
        val existing = fakeExisting(
            isArchived = true,
            unreadCount = 7,
            isMuted = true,
            mutedUntil = -1L,
            disappearingModeSeconds = 3600L,
        )

        val dbo = dto.toDBO(existing = existing)

        assertTrue(dbo.isArchived)
        assertEquals(7, dbo.unreadCount)
        assertTrue(dbo.isMuted)
        assertEquals(-1L, dbo.mutedUntil)
        assertEquals(3600L, dbo.disappearingModeSeconds)
    }

    @Test
    fun `toDBO defaults archive, unread and mute state when no existing row`() {
        val dto = fakeDto()

        val dbo = dto.toDBO(existing = null)

        assertFalse(dbo.isArchived)
        assertEquals(0, dbo.unreadCount)
        assertFalse(dbo.isMuted)
        assertEquals(0L, dbo.mutedUntil)
        assertEquals(0L, dbo.disappearingModeSeconds)
    }

    @Test
    fun `toDBO uses DTO description and avatar when existing is null`() {
        val dto = fakeDto(description = "desc", avatarUrl = "avatar.png")

        val dbo = dto.toDBO(existing = null)

        assertEquals("desc", dbo.description)
        assertEquals("avatar.png", dbo.groupAvatarUrl)
    }

    @Test
    fun `toDBO falls back to existing description and avatar when DTO values are null`() {
        val dto = fakeDto(description = null, avatarUrl = null)
        val existing = fakeExisting(description = "old desc", groupAvatarUrl = "old-avatar.png")

        val dbo = dto.toDBO(existing = existing)

        assertEquals("old desc", dbo.description)
        assertEquals("old-avatar.png", dbo.groupAvatarUrl)
    }

    // ── toBO ─────────────────────────────────────────────────────────────────

    @Test
    fun `toBO defaults name to Chat when DBO name is null`() {
        val dbo = fakeExisting(name = null)

        val bo = dbo.toBO()

        assertEquals("Chat", bo.name)
    }

    @Test
    fun `toBO reflects effective mute state`() {
        val muted = fakeExisting(mutedUntil = -1L).toBO()
        val notMuted = fakeExisting(isMuted = false, mutedUntil = 0L).toBO()

        assertTrue(muted.isMuted)
        assertFalse(notMuted.isMuted)
    }

    @Test
    fun `toBO carries through lastMessage, trailingImageCount and otherUserAvatarUrl`() {
        val dbo = fakeExisting()

        val bo = dbo.toBO(lastMessage = null, trailingImageCount = 3, otherUserAvatarUrl = "other.png")

        assertNull(bo.lastMessage)
        assertEquals(3, bo.trailingImageCount)
        assertEquals("other.png", bo.otherUserAvatarUrl)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun fakeDto(
        name: String? = "Chat Name",
        createdBy: String? = "creator-1",
        updatedAt: String = "2024-01-01T00:00:00Z",
        description: String? = null,
        avatarUrl: String? = null,
    ) = ConversationDTO(
        id = "conv-1",
        name = name,
        isGroup = false,
        createdBy = createdBy,
        updatedAt = updatedAt,
        description = description,
        avatarUrl = avatarUrl,
    )

    private fun fakeExisting(
        name: String? = "Existing Name",
        isArchived: Boolean = false,
        unreadCount: Int = 0,
        isMuted: Boolean = false,
        mutedUntil: Long = 0L,
        disappearingModeSeconds: Long = 0L,
        description: String? = null,
        groupAvatarUrl: String? = null,
    ) = ConversationDBO(
        id = "conv-1",
        name = name,
        isGroup = false,
        createdBy = "creator-1",
        updatedAt = 0L,
        unreadCount = unreadCount,
        description = description,
        groupAvatarUrl = groupAvatarUrl,
        isMuted = isMuted,
        mutedUntil = mutedUntil,
        isArchived = isArchived,
        disappearingModeSeconds = disappearingModeSeconds,
    )
}
