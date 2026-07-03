package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReactionDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: ReactionDao get() = db.reactionDao()

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

    // ── insert / exists ───────────────────────────────────────────────────────

    @Test
    fun `insertReaction stores the reaction and exists returns 1`() = runTest {
        dao.insert(fakeReaction("msg1", "user1", "👍"))

        val count = dao.exists("msg1", "user1", "👍")
        assertEquals(1, count)
    }

    @Test
    fun `exists returns 0 when reaction is not present`() = runTest {
        val count = dao.exists("msg1", "user1", "👍")
        assertEquals(0, count)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete removes the reaction`() = runTest {
        dao.insert(fakeReaction("msg1", "user1", "👍"))
        dao.delete("msg1", "user1", "👍")

        val count = dao.exists("msg1", "user1", "👍")
        assertEquals(0, count)
    }

    @Test
    fun `delete does not remove other reactions`() = runTest {
        dao.insert(fakeReaction("msg1", "user1", "👍"))
        dao.insert(fakeReaction("msg1", "user2", "❤️"))
        dao.delete("msg1", "user1", "👍")

        val remaining = dao.exists("msg1", "user2", "❤️")
        assertEquals(1, remaining)
    }

    // ── insertIgnoreOnDuplicate ───────────────────────────────────────────────

    @Test
    fun `insert with IGNORE strategy does not throw on duplicate`() = runTest {
        dao.insert(fakeReaction("msg1", "user1", "👍"))
        // Should not throw — IGNORE strategy silently skips the duplicate
        dao.insert(fakeReaction("msg1", "user1", "👍"))

        val count = dao.exists("msg1", "user1", "👍")
        assertEquals(1, count)
    }

    // ── observeByConversation ─────────────────────────────────────────────────

    @Test
    fun `observeByConversation returns only reactions for messages in that conversation`() = runTest {
        // observeByConversation uses a subquery: messageId IN (SELECT id FROM messages WHERE conversationId = ?)
        // so we must insert messages first
        val messageDao = db.messageDao()
        messageDao.upsertAll(listOf(
            fakeMessage("msgA", "conv1"),
            fakeMessage("msgB", "conv2"),
        ))

        dao.insert(fakeReaction("msgA", "user1", "👍"))
        dao.insert(fakeReaction("msgB", "user1", "❤️"))

        val result = dao.observeByConversation("conv1").first()
        assertEquals(1, result.size)
        assertEquals("msgA", result[0].messageId)
    }

    @Test
    fun `observeByConversation returns empty when no messages in conversation`() = runTest {
        dao.insert(fakeReaction("msg1", "user1", "👍"))

        val result = dao.observeByConversation("conv_unknown").first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeByConversation returns all reactions for multiple messages in same conversation`() = runTest {
        val messageDao = db.messageDao()
        messageDao.upsertAll(listOf(
            fakeMessage("msg1", "conv1"),
            fakeMessage("msg2", "conv1"),
        ))

        dao.insert(fakeReaction("msg1", "user1", "👍"))
        dao.insert(fakeReaction("msg1", "user2", "❤️"))
        dao.insert(fakeReaction("msg2", "user1", "😂"))

        val result = dao.observeByConversation("conv1").first()
        assertEquals(3, result.size)
    }

    // ── upsertAll ─────────────────────────────────────────────────────────────

    @Test
    fun `upsertAll inserts multiple reactions`() = runTest {
        dao.upsertAll(listOf(
            fakeReaction("msg1", "user1", "👍"),
            fakeReaction("msg1", "user2", "❤️"),
            fakeReaction("msg2", "user1", "😂"),
        ))

        assertEquals(1, dao.exists("msg1", "user1", "👍"))
        assertEquals(1, dao.exists("msg1", "user2", "❤️"))
        assertEquals(1, dao.exists("msg2", "user1", "😂"))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeReaction(
        messageId: String,
        userId: String,
        emoji: String,
    ) = ReactionDBO(
        messageId = messageId,
        userId = userId,
        emoji = emoji,
    )

    private fun fakeMessage(
        id: String,
        conversationId: String,
    ) = MessageDBO(
        id = id,
        conversationId = conversationId,
        senderId = "sender",
        content = "test",
        isRead = false,
        createdAt = System.currentTimeMillis(),
        expiresAt = null,
    )
}
