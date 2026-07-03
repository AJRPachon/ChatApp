package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserMapperTest {

    // ── UserDTO.toBO ──────────────────────────────────────────────────────────

    @Test
    fun `toBO uses email prefix as displayName when displayName is empty`() {
        val dto = fakeDto(displayName = "", email = "alice@example.com")
        val bo = dto.toBO(email = "alice@example.com")
        assertEquals("alice", bo.displayName)
    }

    @Test
    fun `toBO keeps displayName when it is not empty`() {
        val dto = fakeDto(displayName = "Alice Smith")
        val bo = dto.toBO(email = "alice@example.com")
        assertEquals("Alice Smith", bo.displayName)
    }

    @Test
    fun `toBO maps avatarUrl as null when not set`() {
        val dto = fakeDto(avatarUrl = null)
        val bo = dto.toBO()
        assertNull(bo.avatarUrl)
    }

    @Test
    fun `toBO maps avatarUrl when provided`() {
        val dto = fakeDto(avatarUrl = "https://example.com/avatar.png")
        val bo = dto.toBO()
        assertEquals("https://example.com/avatar.png", bo.avatarUrl)
    }

    @Test
    fun `toBO parses createdAt timestamp correctly`() {
        val dto = fakeDto(createdAt = "2024-01-15T10:30:00Z")
        val bo = dto.toBO()
        // 2024-01-15T10:30:00Z in epoch millis
        assertEquals(1705314600000L, bo.createdAt.toEpochMilliseconds())
    }

    @Test
    fun `toBO uses current time for createdAt when createdAt is empty`() {
        val before = System.currentTimeMillis()
        val dto = fakeDto(createdAt = "")
        val bo = dto.toBO()
        val after = System.currentTimeMillis()
        val epochMs = bo.createdAt.toEpochMilliseconds()
        assert(epochMs in before..after) { "Expected $epochMs to be between $before and $after" }
    }

    @Test
    fun `toBO maps lastSeen as null when not set`() {
        val dto = fakeDto(lastSeen = null)
        val bo = dto.toBO()
        assertNull(bo.lastSeen)
    }

    @Test
    fun `toBO maps lastSeen when provided`() {
        val dto = fakeDto(lastSeen = "2024-06-01T08:00:00Z")
        val bo = dto.toBO()
        assertEquals(1717228800000L, bo.lastSeen?.toEpochMilliseconds())
    }

    @Test
    fun `toBO uses empty string for username when null`() {
        val dto = fakeDto(username = null)
        val bo = dto.toBO()
        assertEquals("", bo.username)
    }

    // ── UserDTO.toDBO ─────────────────────────────────────────────────────────

    @Test
    fun `toDBO uses email prefix as displayName when displayName is empty`() {
        val dto = fakeDto(displayName = "")
        val dbo = dto.toDBO(email = "bob@example.com")
        assertEquals("bob", dbo.displayName)
    }

    @Test
    fun `toDBO maps avatarUrl as null when not set`() {
        val dto = fakeDto(avatarUrl = null)
        val dbo = dto.toDBO()
        assertNull(dbo.avatarUrl)
    }

    @Test
    fun `toDBO round-trip id is preserved`() {
        val dto = fakeDto(id = "user-abc-123")
        val dbo = dto.toDBO()
        assertEquals("user-abc-123", dbo.id)
    }

    @Test
    fun `toDBO isCurrentUser defaults to false`() {
        val dto = fakeDto()
        val dbo = dto.toDBO()
        assertEquals(false, dbo.isCurrentUser)
    }

    @Test
    fun `toDBO isCurrentUser can be set to true`() {
        val dto = fakeDto()
        val dbo = dto.toDBO(isCurrentUser = true)
        assertEquals(true, dbo.isCurrentUser)
    }

    // ── UserDBO.toBO ──────────────────────────────────────────────────────────

    @Test
    fun `UserDBO toBO maps all fields correctly`() {
        val dbo = UserDBO(
            id = "u1",
            email = "u1@example.com",
            username = "u1name",
            displayName = "User One",
            avatarUrl = null,
            createdAt = 1000L,
            lastSeen = null,
        )

        val bo = dbo.toBO()

        assertEquals("u1", bo.id)
        assertEquals("u1@example.com", bo.email)
        assertEquals("u1name", bo.username)
        assertEquals("User One", bo.displayName)
        assertNull(bo.avatarUrl)
        assertEquals(1000L, bo.createdAt.toEpochMilliseconds())
        assertNull(bo.lastSeen)
    }

    @Test
    fun `UserDBO toBO maps lastSeen when provided`() {
        val dbo = UserDBO(
            id = "u1",
            email = "u1@example.com",
            username = "u1",
            displayName = "User One",
            avatarUrl = null,
            createdAt = 0L,
            lastSeen = 5000L,
        )

        val bo = dbo.toBO()

        assertEquals(5000L, bo.lastSeen?.toEpochMilliseconds())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun fakeDto(
        id: String = "user1",
        username: String? = "user1",
        displayName: String = "User One",
        avatarUrl: String? = null,
        createdAt: String = "2024-01-01T00:00:00Z",
        lastSeen: String? = null,
        email: String = "",
    ) = UserDTO(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        createdAt = createdAt,
        lastSeen = lastSeen,
    )
}
