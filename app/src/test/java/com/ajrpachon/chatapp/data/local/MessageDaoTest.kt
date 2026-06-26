package com.ajrpachon.chatapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import android.app.Application
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
class MessageDaoTest {

    private lateinit var db: ChatDatabase
    private val messageDao get() = db.messageDao()

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

    // ── upsert / observe ──────────────────────────────────────────────────────

    @Test
    fun `upsert and observe messages returns inserted messages`() = runTest {
        val msg = fakeMessage("msg1", "conv1")
        messageDao.upsert(msg)

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals(1, result.size)
        assertEquals("msg1", result[0].id)
    }

    @Test
    fun `upsert replaces existing message with same id`() = runTest {
        messageDao.upsert(fakeMessage("msg1", "conv1", content = "original"))
        messageDao.upsert(fakeMessage("msg1", "conv1", content = "updated"))

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals(1, result.size)
        assertEquals("updated", result[0].content)
    }

    @Test
    fun `upsertAll inserts multiple messages`() = runTest {
        messageDao.upsertAll(listOf(
            fakeMessage("m1", "conv1"),
            fakeMessage("m2", "conv1"),
            fakeMessage("m3", "conv2"),
        ))

        val conv1 = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals(2, conv1.size)
    }

    // ── markDeleted ───────────────────────────────────────────────────────────

    @Test
    fun `markDeleted sets isDeleted flag`() = runTest {
        messageDao.upsert(fakeMessage("msg1", "conv1"))
        messageDao.markDeleted("msg1")

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertTrue(result[0].isDeleted)
    }

    // ── markAllRead ───────────────────────────────────────────────────────────

    @Test
    fun `markAllRead marks all messages in conversation as read`() = runTest {
        messageDao.upsertAll(listOf(
            fakeMessage("m1", "conv1", isRead = false),
            fakeMessage("m2", "conv1", isRead = false),
        ))
        messageDao.markAllRead("conv1")

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertTrue(result.all { it.isRead })
    }

    // ── updateContent ─────────────────────────────────────────────────────────

    @Test
    fun `updateContent edits content and sets isEdited flag`() = runTest {
        messageDao.upsert(fakeMessage("msg1", "conv1", content = "hello"))
        messageDao.updateContent("msg1", "hello edited", 999L)

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals("hello edited", result[0].content)
        assertTrue(result[0].isEdited)
        assertEquals(999L, result[0].editedAt)
    }

    // ── expiry ────────────────────────────────────────────────────────────────

    @Test
    fun `setExpiry stores epoch millis on message`() = runTest {
        messageDao.upsert(fakeMessage("msg1", "conv1"))
        messageDao.setExpiry("msg1", 12345L)

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals(12345L, result[0].expiresAt)
    }

    @Test
    fun `setExpiry with null clears expiry`() = runTest {
        messageDao.upsert(fakeMessage("msg1", "conv1", expiresAt = 12345L))
        messageDao.setExpiry("msg1", null)

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertNull(result[0].expiresAt)
    }

    @Test
    fun `deleteExpired removes messages whose expiresAt is in the past`() = runTest {
        val now = System.currentTimeMillis()
        messageDao.upsertAll(listOf(
            fakeMessage("expired", "conv1", expiresAt = now - 1000L),
            fakeMessage("future", "conv1", expiresAt = now + 60_000L),
            fakeMessage("permanent", "conv1", expiresAt = null),
        ))

        messageDao.deleteExpired(now)

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals(2, result.size)
        assertTrue(result.none { it.id == "expired" })
    }

    @Test
    fun `deleteExpired does not touch messages without expiry`() = runTest {
        messageDao.upsert(fakeMessage("msg1", "conv1", expiresAt = null))
        messageDao.deleteExpired(System.currentTimeMillis())

        val result = messageDao.observeByConversation("conv1", 0L).first()
        assertEquals(1, result.size)
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    fun `searchMessages returns messages matching query`() = runTest {
        messageDao.upsertAll(listOf(
            fakeMessage("m1", "conv1", content = "hello world"),
            fakeMessage("m2", "conv1", content = "goodbye world"),
            fakeMessage("m3", "conv1", content = "nothing relevant"),
        ))

        val results = messageDao.searchMessages("conv1", "world")
        assertEquals(2, results.size)
    }

    @Test
    fun `searchMessages is case-insensitive for ASCII`() = runTest {
        messageDao.upsert(fakeMessage("m1", "conv1", content = "Hello World"))
        val results = messageDao.searchMessages("conv1", "hello")
        assertEquals(1, results.size)
    }

    @Test
    fun `searchMessages scoped to conversation`() = runTest {
        messageDao.upsertAll(listOf(
            fakeMessage("m1", "conv1", content = "target"),
            fakeMessage("m2", "conv2", content = "target"),
        ))
        val results = messageDao.searchMessages("conv1", "target")
        assertEquals(1, results.size)
        assertEquals("m1", results[0].id)
    }

    // ── getLastMessage ────────────────────────────────────────────────────────

    @Test
    fun `getLastMessage returns most recent message`() = runTest {
        messageDao.upsertAll(listOf(
            fakeMessage("old", "conv1", createdAt = 100L),
            fakeMessage("new", "conv1", createdAt = 200L),
        ))
        val last = messageDao.getLastMessage("conv1")
        assertNotNull(last)
        assertEquals("new", last!!.id)
    }

    // ── deleteByConversation ──────────────────────────────────────────────────

    @Test
    fun `deleteByConversation removes all messages for that conversation`() = runTest {
        messageDao.upsertAll(listOf(
            fakeMessage("m1", "conv1"),
            fakeMessage("m2", "conv1"),
            fakeMessage("m3", "conv2"),
        ))
        messageDao.deleteByConversation("conv1")

        val conv1 = messageDao.observeByConversation("conv1", 0L).first()
        val conv2 = messageDao.observeByConversation("conv2", 0L).first()
        assertTrue(conv1.isEmpty())
        assertEquals(1, conv2.size)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeMessage(
        id: String,
        conversationId: String,
        content: String = "test",
        isRead: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        expiresAt: Long? = null,
    ) = MessageDBO(
        id = id,
        conversationId = conversationId,
        senderId = "sender",
        content = content,
        isRead = isRead,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )
}
