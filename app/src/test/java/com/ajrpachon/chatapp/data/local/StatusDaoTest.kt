package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.StatusDao
import com.ajrpachon.chatapp.data.local.entity.StatusDBO
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
class StatusDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: StatusDao get() = db.statusDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    // ── observeActive ─────────────────────────────────────────────────────────

    @Test
    fun `observeActive returns only non-expired statuses`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "user1", expiresAt = now + 60_000L),
            fakeStatus("s2", "user2", expiresAt = now - 1_000L),
        ))

        val active = dao.observeActive(now).first()
        assertEquals(1, active.size)
        assertEquals("s1", active[0].id)
    }

    @Test
    fun `observeActive returns empty when all statuses expired`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "user1", expiresAt = now - 5_000L),
            fakeStatus("s2", "user2", expiresAt = now - 1_000L),
        ))

        val active = dao.observeActive(now).first()
        assertTrue(active.isEmpty())
    }

    @Test
    fun `observeActive returns empty when table is empty`() = runTest {
        val active = dao.observeActive(System.currentTimeMillis()).first()
        assertTrue(active.isEmpty())
    }

    // ── upsert / upsertAll ────────────────────────────────────────────────────

    @Test
    fun `upsert replaces existing status with same id`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsert(fakeStatus("s1", "user1", text = "original", expiresAt = now + 60_000L))
        dao.upsert(fakeStatus("s1", "user1", text = "updated", expiresAt = now + 60_000L))

        val result = dao.observeActive(now).first()
        assertEquals(1, result.size)
        assertEquals("updated", result[0].text)
    }

    @Test
    fun `upsertAll inserts multiple statuses`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "user1", expiresAt = now + 60_000L),
            fakeStatus("s2", "user2", expiresAt = now + 60_000L),
            fakeStatus("s3", "user3", expiresAt = now + 60_000L),
        ))

        val active = dao.observeActive(now).first()
        assertEquals(3, active.size)
    }

    // ── deleteExpired ─────────────────────────────────────────────────────────

    @Test
    fun `deleteExpired removes expired statuses`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "user1", expiresAt = now - 1_000L),
            fakeStatus("s2", "user2", expiresAt = now + 60_000L),
        ))

        dao.deleteExpired(now)

        val active = dao.observeActive(now).first()
        assertEquals(1, active.size)
        assertEquals("s2", active[0].id)
    }

    @Test
    fun `deleteExpired is a no-op when nothing is expired`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "user1", expiresAt = now + 60_000L),
            fakeStatus("s2", "user2", expiresAt = now + 120_000L),
        ))

        dao.deleteExpired(now)

        val active = dao.observeActive(now).first()
        assertEquals(2, active.size)
    }

    // ── getByUser ─────────────────────────────────────────────────────────────

    @Test
    fun `getByUser returns only statuses for that user`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "alice", expiresAt = now + 60_000L),
            fakeStatus("s2", "bob", expiresAt = now + 60_000L),
            fakeStatus("s3", "alice", expiresAt = now + 120_000L),
        ))

        val aliceStatuses = dao.getByUser("alice", now)
        assertEquals(2, aliceStatuses.size)
        assertTrue(aliceStatuses.all { it.userId == "alice" })
    }

    @Test
    fun `getByUser returns empty list for unknown user`() = runTest {
        val result = dao.getByUser("nobody", System.currentTimeMillis())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getByUser excludes expired statuses`() = runTest {
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            fakeStatus("s1", "alice", expiresAt = now + 60_000L),
            fakeStatus("s2", "alice", expiresAt = now - 1_000L),
        ))

        val result = dao.getByUser("alice", now)
        assertEquals(1, result.size)
        assertEquals("s1", result[0].id)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeStatus(
        id: String,
        userId: String,
        text: String? = "hello",
        expiresAt: Long = System.currentTimeMillis() + 60_000L,
    ) = StatusDBO(
        id = id,
        userId = userId,
        text = text,
        imageUrl = null,
        backgroundColor = 0xFF1976D2,
        createdAt = System.currentTimeMillis(),
        expiresAt = expiresAt,
    )
}
