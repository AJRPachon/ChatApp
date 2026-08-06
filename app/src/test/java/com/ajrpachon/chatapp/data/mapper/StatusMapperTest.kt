package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.StatusDBO
import com.ajrpachon.chatapp.data.remote.dto.StatusDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusMapperTest {

    private val statusTtlMs = 24 * 60 * 60 * 1000L

    // ── toDBO ────────────────────────────────────────────────────────────────

    @Test
    fun `toDBO parses valid createdAt and expiresAt timestamps`() {
        val dto = fakeDto(createdAt = "2024-01-01T00:00:00Z", expiresAt = "2024-01-02T00:00:00Z")

        val dbo = dto.toDBO()

        assertEquals(1704067200000L, dbo.createdAt)
        assertEquals(1704153600000L, dbo.expiresAt)
    }

    @Test
    fun `toDBO falls back to current time when createdAt is malformed`() {
        val dto = fakeDto(createdAt = "garbage")
        val before = System.currentTimeMillis()

        val dbo = dto.toDBO()

        assertTrue(dbo.createdAt >= before)
    }

    @Test
    fun `toDBO falls back to now plus TTL when expiresAt is malformed`() {
        val dto = fakeDto(expiresAt = "garbage")
        val before = System.currentTimeMillis() + statusTtlMs

        val dbo = dto.toDBO()

        assertTrue(dbo.expiresAt >= before)
    }

    @Test
    fun `toDBO copies text, imageUrl and backgroundColor unchanged`() {
        val dto = fakeDto(text = "Hello", imageUrl = "img.png", backgroundColor = 0xFF0000)

        val dbo = dto.toDBO()

        assertEquals("Hello", dbo.text)
        assertEquals("img.png", dbo.imageUrl)
        assertEquals(0xFF0000L, dbo.backgroundColor)
    }

    // ── toBO ─────────────────────────────────────────────────────────────────

    @Test
    fun `toBO maps epoch millis back to Instant and carries display fields`() {
        val dbo = fakeDbo(createdAt = 1704067200000L, expiresAt = 1704153600000L)

        val bo = dbo.toBO(userName = "Alice", userAvatarUrl = "avatar.png", isFromMe = true)

        assertEquals(1704067200000L, bo.createdAt.toEpochMilliseconds())
        assertEquals(1704153600000L, bo.expiresAt.toEpochMilliseconds())
        assertEquals("Alice", bo.userName)
        assertEquals("avatar.png", bo.userAvatarUrl)
        assertTrue(bo.isFromMe)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun fakeDto(
        text: String? = "status text",
        imageUrl: String? = null,
        backgroundColor: Long = 0xFF1976D2,
        createdAt: String = "2024-01-01T00:00:00Z",
        expiresAt: String = "2024-01-02T00:00:00Z",
    ) = StatusDTO(
        id = "status-1",
        userId = "user-1",
        text = text,
        imageUrl = imageUrl,
        backgroundColor = backgroundColor,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )

    private fun fakeDbo(
        createdAt: Long = 0L,
        expiresAt: Long = 0L,
    ) = StatusDBO(
        id = "status-1",
        userId = "user-1",
        text = "status text",
        imageUrl = null,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )
}
