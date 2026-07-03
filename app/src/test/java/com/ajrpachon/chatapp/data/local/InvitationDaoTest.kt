package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
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
class InvitationDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: InvitationDao get() = db.invitationDao()

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

    // ── observePending ────────────────────────────────────────────────────────

    @Test
    fun `observePending returns only pending invitations for receiver`() = runTest {
        dao.upsertAll(listOf(
            fakeInvitation("inv1", receiverId = "alice", status = "pending"),
            fakeInvitation("inv2", receiverId = "alice", status = "rejected"),
            fakeInvitation("inv3", receiverId = "alice", status = "accepted"),
        ))

        val result = dao.observePending("alice").first()
        assertEquals(1, result.size)
        assertEquals("inv1", result[0].id)
    }

    @Test
    fun `observePending excludes invitations for other receivers`() = runTest {
        dao.upsertAll(listOf(
            fakeInvitation("inv1", receiverId = "alice", status = "pending"),
            fakeInvitation("inv2", receiverId = "bob", status = "pending"),
        ))

        val result = dao.observePending("alice").first()
        assertEquals(1, result.size)
        assertEquals("inv1", result[0].id)
    }

    @Test
    fun `observePending returns empty when no pending invitations`() = runTest {
        dao.upsert(fakeInvitation("inv1", receiverId = "alice", status = "accepted"))

        val result = dao.observePending("alice").first()
        assertTrue(result.isEmpty())
    }

    // ── insertAndObserve ──────────────────────────────────────────────────────

    @Test
    fun `upsert and observe returns inserted invitation`() = runTest {
        dao.upsert(fakeInvitation("inv1", receiverId = "alice", status = "pending"))

        val result = dao.observePending("alice").first()
        assertEquals(1, result.size)
        assertEquals("inv1", result[0].id)
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    fun `updateStatus changes pending to accepted`() = runTest {
        dao.upsert(fakeInvitation("inv1", receiverId = "alice", status = "pending"))
        dao.updateStatus("inv1", "accepted")

        val pending = dao.observePending("alice").first()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun `updateStatus does not affect other invitations`() = runTest {
        dao.upsertAll(listOf(
            fakeInvitation("inv1", receiverId = "alice", status = "pending"),
            fakeInvitation("inv2", receiverId = "alice", status = "pending"),
        ))
        dao.updateStatus("inv1", "accepted")

        val pending = dao.observePending("alice").first()
        assertEquals(1, pending.size)
        assertEquals("inv2", pending[0].id)
    }

    // ── ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `observePending orders by createdAt descending`() = runTest {
        dao.upsertAll(listOf(
            fakeInvitation("inv1", receiverId = "alice", status = "pending", createdAt = 100L),
            fakeInvitation("inv2", receiverId = "alice", status = "pending", createdAt = 300L),
            fakeInvitation("inv3", receiverId = "alice", status = "pending", createdAt = 200L),
        ))

        val result = dao.observePending("alice").first()
        assertEquals(listOf("inv2", "inv3", "inv1"), result.map { it.id })
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeInvitation(
        id: String,
        senderId: String = "sender1",
        receiverId: String = "receiver1",
        status: String = "pending",
        createdAt: Long = System.currentTimeMillis(),
    ) = InvitationDBO(
        id = id,
        senderId = senderId,
        senderUsername = senderId,
        senderDisplayName = "Sender $senderId",
        receiverId = receiverId,
        status = status,
        createdAt = createdAt,
    )
}
