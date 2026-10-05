package com.ajrpachon.chatapp.ui.chat

import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.model.GroupRole
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMentionsTest {

    // ── activeQuery ───────────────────────────────────────────────────────────

    @Test
    fun `activeQuery returns the partial username after a trailing at-sign token`() {
        assertEquals("an", ChatMentions.activeQuery("hola @an"))
    }

    @Test
    fun `activeQuery is empty for a bare at-sign`() {
        assertEquals("", ChatMentions.activeQuery("hola @"))
    }

    @Test
    fun `activeQuery is null once the mention is closed by a space`() {
        assertNull(ChatMentions.activeQuery("hola @ana "))
    }

    @Test
    fun `activeQuery is null inside an e-mail address`() {
        assertNull(ChatMentions.activeQuery("escribe a ana@mail.com"))
    }

    @Test
    fun `activeQuery looks at the token after a newline too`() {
        assertEquals("be", ChatMentions.activeQuery("hola\n@be"))
    }

    // ── suggestions ───────────────────────────────────────────────────────────

    @Test
    fun `suggestions match username or display name case-insensitively, prefix matches first`() {
        val members = listOf(
            member("u1", username = "carlos", displayName = "Carlos Ana"),
            member("u2", username = "ana", displayName = "Ana"),
            member("u3", username = "bea", displayName = "Bea"),
        )

        val result = ChatMentions.suggestions("@AN", members, currentUserId = null)

        assertEquals(listOf("ana", "carlos"), result.map { it.username })
    }

    @Test
    fun `suggestions exclude the current user and members without username`() {
        val members = listOf(member("me", username = "ana"), member("u2", username = ""), member("u3", username = "andres"))

        val result = ChatMentions.suggestions("@an", members, currentUserId = "me")

        assertEquals(listOf("andres"), result.map { it.username })
    }

    @Test
    fun `suggestions are empty when no query is active`() {
        assertTrue(ChatMentions.suggestions("hola", listOf(member("u1", username = "ana")), null).isEmpty())
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    fun `insert replaces only the trailing at-query and closes it with a space`() {
        assertEquals("ana@mail.com y @andres ", ChatMentions.insert("ana@mail.com y @and", "andres"))
    }

    @Test
    fun `insert leaves text untouched when no query is active`() {
        assertEquals("hola", ChatMentions.insert("hola", "ana"))
    }

    // ── mentionRanges ─────────────────────────────────────────────────────────

    @Test
    fun `mentionRanges finds mentions but not e-mail addresses`() {
        val text = "@ana y @Bea_2, escribid a x@mail.com"

        val mentions = ChatMentions.mentionRanges(text).map { text.substring(it) }

        assertEquals(listOf("@ana", "@Bea_2"), mentions)
    }

    private fun member(userId: String, username: String = userId, displayName: String = username) = GroupMemberBO(
        userId = userId,
        conversationId = "conv1",
        displayName = displayName,
        username = username,
        avatarUrl = null,
        role = GroupRole.MEMBER,
        joinedAt = Instant.fromEpochMilliseconds(0),
    )
}
