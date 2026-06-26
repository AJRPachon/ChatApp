package com.ajrpachon.chatapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConversationDaoTest {

    private lateinit var db: ChatDatabase
    private val dao get() = db.conversationDao()

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
        stopKoin()
    }

    // ── upsert / observeAll ───────────────────────────────────────────────────

    @Test
    fun `upsert and observe returns inserted conversation`() = runTest {
        dao.upsert(fakeConversation("c1"))

        val result = dao.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("c1", result[0].id)
    }

    @Test
    fun `upsertAll inserts multiple conversations`() = runTest {
        dao.upsertAll(listOf(fakeConversation("c1"), fakeConversation("c2"), fakeConversation("c3")))

        val result = dao.observeAll().first()
        assertEquals(3, result.size)
    }

    @Test
    fun `upsert replaces conversation with same id`() = runTest {
        dao.upsert(fakeConversation("c1", name = "Old Name"))
        dao.upsert(fakeConversation("c1", name = "New Name"))

        val result = dao.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("New Name", result[0].name)
    }

    @Test
    fun `observeAll orders by updatedAt descending`() = runTest {
        dao.upsertAll(listOf(
            fakeConversation("c1", updatedAt = 100L),
            fakeConversation("c2", updatedAt = 300L),
            fakeConversation("c3", updatedAt = 200L),
        ))

        val result = dao.observeAll().first()
        assertEquals(listOf("c2", "c3", "c1"), result.map { it.id })
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun `getById returns correct conversation`() = runTest {
        dao.upsertAll(listOf(fakeConversation("c1"), fakeConversation("c2")))

        val result = dao.getById("c1")
        assertNotNull(result)
        assertEquals("c1", result!!.id)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        val result = dao.getById("nonexistent")
        assertNull(result)
    }

    // ── deleteById ────────────────────────────────────────────────────────────

    @Test
    fun `deleteById removes conversation`() = runTest {
        dao.upsertAll(listOf(fakeConversation("c1"), fakeConversation("c2")))
        dao.deleteById("c1")

        val result = dao.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("c2", result[0].id)
    }

    // ── mute ─────────────────────────────────────────────────────────────────

    @Test
    fun `updateMuted sets isMuted flag`() = runTest {
        dao.upsert(fakeConversation("c1", isMuted = false))
        dao.updateMuted("c1", true)

        val result = dao.getById("c1")
        assertTrue(result!!.isMuted)
    }

    @Test
    fun `updateMutedUntil sets mutedUntil and syncs isMuted`() = runTest {
        dao.upsert(fakeConversation("c1"))
        val future = System.currentTimeMillis() + 3_600_000L
        dao.updateMutedUntil("c1", future)

        val result = dao.getById("c1")!!
        assertEquals(future, result.mutedUntil)
        assertTrue(result.isMuted)
    }

    @Test
    fun `updateMutedUntil with 0 clears mute`() = runTest {
        dao.upsert(fakeConversation("c1", isMuted = true, mutedUntil = 9999L))
        dao.updateMutedUntil("c1", 0L)

        val result = dao.getById("c1")!!
        assertEquals(0L, result.mutedUntil)
        assertFalse(result.isMuted)
    }

    // ── isEffectivelyMuted ────────────────────────────────────────────────────

    @Test
    fun `isEffectivelyMuted true when isMuted is true`() {
        val conv = fakeConversation("c1", isMuted = true)
        assertTrue(conv.isEffectivelyMuted())
    }

    @Test
    fun `isEffectivelyMuted true when mutedUntil is -1 (forever)`() {
        val conv = fakeConversation("c1", mutedUntil = -1L)
        assertTrue(conv.isEffectivelyMuted())
    }

    @Test
    fun `isEffectivelyMuted true when mutedUntil is in the future`() {
        val conv = fakeConversation("c1", mutedUntil = System.currentTimeMillis() + 60_000L)
        assertTrue(conv.isEffectivelyMuted())
    }

    @Test
    fun `isEffectivelyMuted false when mutedUntil is in the past`() {
        val conv = fakeConversation("c1", isMuted = false, mutedUntil = 1L)
        assertFalse(conv.isEffectivelyMuted())
    }

    @Test
    fun `isEffectivelyMuted false when neither muted flag set`() {
        val conv = fakeConversation("c1")
        assertFalse(conv.isEffectivelyMuted())
    }

    // ── getByOtherUserId ──────────────────────────────────────────────────────

    @Test
    fun `getByOtherUserId returns matching conversation`() = runTest {
        dao.upsert(fakeConversation("c1", otherUserId = "user42"))
        val result = dao.getByOtherUserId("user42")
        assertNotNull(result)
        assertEquals("c1", result!!.id)
    }

    @Test
    fun `getByOtherUserId returns null when no match`() = runTest {
        val result = dao.getByOtherUserId("unknown")
        assertNull(result)
    }

    // ── resetUnreadCount ──────────────────────────────────────────────────────

    @Test
    fun `resetUnreadCount sets unreadCount to zero`() = runTest {
        dao.upsert(fakeConversation("c1", unreadCount = 5))
        dao.resetUnreadCount("c1")

        val result = dao.getById("c1")!!
        assertEquals(0, result.unreadCount)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeConversation(
        id: String,
        name: String? = "Test Conv",
        updatedAt: Long = System.currentTimeMillis(),
        isMuted: Boolean = false,
        mutedUntil: Long = 0L,
        otherUserId: String? = null,
        unreadCount: Int = 0,
    ) = ConversationDBO(
        id = id,
        name = name,
        isGroup = false,
        createdBy = "creator",
        updatedAt = updatedAt,
        isMuted = isMuted,
        mutedUntil = mutedUntil,
        otherUserId = otherUserId,
        unreadCount = unreadCount,
    )
}
