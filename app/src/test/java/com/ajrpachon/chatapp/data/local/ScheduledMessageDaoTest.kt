package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.ScheduledMessageDao
import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ScheduledMessageDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: ScheduledMessageDao get() = db.scheduledMessageDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun message(id: String, at: Long) =
        ScheduledMessageDBO(id = id, conversationId = "c1", senderId = "me", text = "t-$id", scheduledAtMs = at, createdAt = 0L)

    @Test
    fun `observeAll and getAll are ordered by scheduled time`() = runTest {
        dao.insert(message("late", 300L))
        dao.insert(message("early", 100L))
        dao.insert(message("mid", 200L))

        assertEquals(listOf("early", "mid", "late"), dao.observeAll().first().map { it.id })
        assertEquals(listOf("early", "mid", "late"), dao.getAll().map { it.id })
    }

    @Test
    fun `getPending returns the messages due at or before now`() = runTest {
        dao.insert(message("due", 100L))
        dao.insert(message("exact", 200L))
        dao.insert(message("future", 201L))

        assertEquals(listOf("due", "exact"), dao.getPending(200L).map { it.id })
    }

    @Test
    fun `insert replaces a message with the same id`() = runTest {
        dao.insert(message("m1", 100L))
        dao.insert(message("m1", 500L))

        assertEquals(500L, dao.getAll().single().scheduledAtMs)
    }

    @Test
    fun `deleteById removes only that message`() = runTest {
        dao.insert(message("a", 1L))
        dao.insert(message("b", 2L))

        dao.deleteById("a")

        assertEquals(listOf("b"), dao.getAll().map { it.id })
    }
}
