package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.GroupMemberDao
import com.ajrpachon.chatapp.data.local.entity.GroupMemberDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GroupMemberDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: GroupMemberDao get() = db.groupMemberDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── observeByConversation ─────────────────────────────────────────────────

    @Test
    fun `observeByConversation returns only members for that conversation`() = runTest {
        dao.upsertAll(listOf(
            fakeMember("conv_a", "user1"),
            fakeMember("conv_a", "user2"),
            fakeMember("conv_b", "user3"),
        ))

        val result = dao.observeByConversation("conv_a").first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.conversationId == "conv_a" })
    }

    @Test
    fun `observeByConversation returns empty when conversation has no members`() = runTest {
        dao.upsert(fakeMember("conv_a", "user1"))

        val result = dao.observeByConversation("conv_b").first()
        assertTrue(result.isEmpty())
    }

    // ── deleteAllForConversation ──────────────────────────────────────────────

    @Test
    fun `deleteAllForConversation removes all members for that conversation`() = runTest {
        dao.upsertAll(listOf(
            fakeMember("conv_a", "user1"),
            fakeMember("conv_a", "user2"),
            fakeMember("conv_b", "user3"),
        ))
        dao.deleteAllForConversation("conv_a")

        val convA = dao.observeByConversation("conv_a").first()
        val convB = dao.observeByConversation("conv_b").first()
        assertTrue(convA.isEmpty())
        assertEquals(1, convB.size)
    }

    @Test
    fun `deleteAllForConversation is no-op when conversation has no members`() = runTest {
        dao.upsert(fakeMember("conv_a", "user1"))
        dao.deleteAllForConversation("conv_b")

        val result = dao.observeByConversation("conv_a").first()
        assertEquals(1, result.size)
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    fun `upsert inserts new member`() = runTest {
        dao.upsert(fakeMember("conv_a", "user1", role = "member"))

        val result = dao.getByUser("conv_a", "user1")
        assertNotNull(result)
        assertEquals("member", result!!.role)
    }

    @Test
    fun `upsert replaces existing member updating role`() = runTest {
        dao.upsert(fakeMember("conv_a", "user1", role = "member"))
        dao.upsert(fakeMember("conv_a", "user1", role = "admin"))

        val result = dao.getByUser("conv_a", "user1")
        assertNotNull(result)
        assertEquals("admin", result!!.role)
    }

    // ── getRole ───────────────────────────────────────────────────────────────

    @Test
    fun `getRole returns the member role`() = runTest {
        dao.upsert(fakeMember("conv_a", "user1", role = "admin"))

        val role = dao.getRole("conv_a", "user1")
        assertEquals("admin", role)
    }

    @Test
    fun `getRole returns null when member does not exist`() = runTest {
        val role = dao.getRole("conv_a", "unknown")
        assertNull(role)
    }

    // ── getAllForConversation ─────────────────────────────────────────────────

    @Test
    fun `getAllForConversation returns all members as a list`() = runTest {
        dao.upsertAll(listOf(
            fakeMember("conv_a", "user1"),
            fakeMember("conv_a", "user2"),
            fakeMember("conv_a", "user3"),
        ))

        val result = dao.getAllForConversation("conv_a")
        assertEquals(3, result.size)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete removes a single member from conversation`() = runTest {
        dao.upsertAll(listOf(
            fakeMember("conv_a", "user1"),
            fakeMember("conv_a", "user2"),
        ))
        dao.delete("conv_a", "user1")

        val result = dao.getAllForConversation("conv_a")
        assertEquals(1, result.size)
        assertEquals("user2", result[0].userId)
    }

    // ── replaceAll ────────────────────────────────────────────────────────────

    @Test
    fun `replaceAll atomically replaces member list for conversation`() = runTest {
        dao.upsertAll(listOf(
            fakeMember("conv_a", "user1"),
            fakeMember("conv_a", "user2"),
        ))

        val newMembers = listOf(
            fakeMember("conv_a", "user3"),
            fakeMember("conv_a", "user4"),
        )
        dao.replaceAll("conv_a", newMembers)

        val result = dao.getAllForConversation("conv_a")
        assertEquals(2, result.size)
        assertTrue(result.map { it.userId }.containsAll(listOf("user3", "user4")))
    }

    @Test
    fun `replaceAll with empty list clears all members`() = runTest {
        dao.upsertAll(listOf(
            fakeMember("conv_a", "user1"),
            fakeMember("conv_a", "user2"),
        ))

        dao.replaceAll("conv_a", emptyList())

        val result = dao.getAllForConversation("conv_a")
        assertTrue(result.isEmpty())
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeMember(
        conversationId: String,
        userId: String,
        role: String = "member",
    ) = GroupMemberDBO(
        conversationId = conversationId,
        userId = userId,
        displayName = "User $userId",
        username = userId,
        avatarUrl = null,
        role = role,
        joinedAt = System.currentTimeMillis(),
    )
}
