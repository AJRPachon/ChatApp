package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.SessionDao
import com.ajrpachon.chatapp.data.local.entity.SessionDBO
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
class SessionDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: SessionDao get() = db.sessionDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun session(id: String, lastActive: Long, current: Boolean = false) =
        SessionDBO(id = id, deviceInfo = "device-$id", createdAt = 1L, lastActiveAt = lastActive, isCurrent = current)

    @Test
    fun `observeAll returns the most recently active session first`() = runTest {
        dao.upsert(session("old", lastActive = 10L))
        dao.upsert(session("new", lastActive = 20L))

        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `upsert replaces a session with the same id`() = runTest {
        dao.upsert(session("s1", lastActive = 10L))
        dao.upsert(session("s1", lastActive = 99L))

        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(99L, all.single().lastActiveAt)
    }

    @Test
    fun `delete removes only the given session`() = runTest {
        dao.upsert(session("s1", 1L))
        dao.upsert(session("s2", 2L))

        dao.delete("s1")

        assertEquals(listOf("s2"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `deleteAllOthers keeps the current session`() = runTest {
        dao.upsert(session("cur", 1L, current = true))
        dao.upsert(session("other", 2L))

        dao.deleteAllOthers()

        assertEquals(listOf("cur"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `deleteAll empties the table`() = runTest {
        dao.upsert(session("cur", 1L, current = true))
        dao.upsert(session("other", 2L))

        dao.deleteAll()

        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun `updateCurrentLastActive only touches the current session`() = runTest {
        dao.upsert(session("cur", 1L, current = true))
        dao.upsert(session("other", 2L))

        dao.updateCurrentLastActive(500L)

        val byId = dao.observeAll().first().associateBy { it.id }
        assertEquals(500L, byId.getValue("cur").lastActiveAt)
        assertEquals(2L, byId.getValue("other").lastActiveAt)
    }
}
