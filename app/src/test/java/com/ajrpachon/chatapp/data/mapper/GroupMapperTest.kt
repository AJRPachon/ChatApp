package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import com.ajrpachon.chatapp.data.remote.dto.GroupMemberDTO
import com.ajrpachon.chatapp.data.remote.dto.GroupMemberProfileDTO
import com.ajrpachon.chatapp.domain.model.GroupRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupMapperTest {

    // ── toDBO ────────────────────────────────────────────────────────────────

    @Test
    fun `toDBO uses profile fields when profile is present`() {
        val dto = fakeDto(profile = GroupMemberProfileDTO(username = "alice_u", displayName = "Alice", avatarUrl = "a.png"))

        val dbo = dto.toDBO()

        assertEquals("Alice", dbo.displayName)
        assertEquals("alice_u", dbo.username)
        assertEquals("a.png", dbo.avatarUrl)
    }

    @Test
    fun `toDBO falls back to userId as displayName and empty username when profile is null`() {
        val dto = fakeDto(userId = "user-42", profile = null)

        val dbo = dto.toDBO()

        assertEquals("user-42", dbo.displayName)
        assertEquals("", dbo.username)
        assertNull(dbo.avatarUrl)
    }

    @Test
    fun `toDBO parses a valid joinedAt timestamp`() {
        val dto = fakeDto(joinedAt = "2024-01-01T00:00:00Z")

        val dbo = dto.toDBO()

        assertEquals(1704067200000L, dbo.joinedAt)
    }

    @Test
    fun `toDBO falls back to current time when joinedAt is malformed`() {
        val dto = fakeDto(joinedAt = "not-a-date")
        val before = System.currentTimeMillis()

        val dbo = dto.toDBO()

        assertTrue(dbo.joinedAt >= before)
    }

    @Test
    fun `toDBO preserves role string as-is`() {
        val dto = fakeDto(role = "admin")

        val dbo = dto.toDBO()

        assertEquals("admin", dbo.role)
    }

    // ── toBO ─────────────────────────────────────────────────────────────────

    @Test
    fun `toBO maps admin role string to ADMIN`() {
        val dbo = fakeDbo(role = "admin")

        val bo = dbo.toBO()

        assertEquals(GroupRole.ADMIN, bo.role)
    }

    @Test
    fun `toBO maps member role string to MEMBER`() {
        val dbo = fakeDbo(role = "member")

        val bo = dbo.toBO()

        assertEquals(GroupRole.MEMBER, bo.role)
    }

    @Test
    fun `toBO maps unrecognized role string to MEMBER`() {
        val dbo = fakeDbo(role = "owner")

        val bo = dbo.toBO()

        assertEquals(GroupRole.MEMBER, bo.role)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun fakeDto(
        userId: String = "user-1",
        role: String = "member",
        joinedAt: String = "2024-01-01T00:00:00Z",
        profile: GroupMemberProfileDTO? = null,
    ) = GroupMemberDTO(
        conversationId = "conv-1",
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        profile = profile,
    )

    private fun fakeDbo(role: String) = GroupMemberDBO(
        conversationId = "conv-1",
        userId = "user-1",
        displayName = "User",
        username = "user",
        avatarUrl = null,
        role = role,
        joinedAt = 0L,
    )
}
