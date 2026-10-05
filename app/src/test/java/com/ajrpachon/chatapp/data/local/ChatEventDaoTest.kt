package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.ChatEventDao
import com.ajrpachon.chatapp.data.local.entity.ChatEventDBO
import com.ajrpachon.chatapp.data.local.entity.EventRsvpDBO
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
class ChatEventDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: ChatEventDao get() = db.chatEventDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun event(id: String, title: String = "Cena") =
        ChatEventDBO(id = id, conversationId = "c1", title = title, dateMs = 100L, createdBy = "me", createdAt = 0L)

    @Test
    fun `an event is found by id and missing ids give null`() = runTest {
        dao.insertEvent(event("e1"))

        assertEquals("Cena", dao.observeById("e1").first()?.title)
        assertNull(dao.observeById("nope").first())
    }

    @Test
    fun `inserting an event again replaces it`() = runTest {
        dao.insertEvent(event("e1", "Cena"))
        dao.insertEvent(event("e1", "Comida"))

        assertEquals("Comida", dao.observeById("e1").first()?.title)
    }

    @Test
    fun `attendees are listed per event`() = runTest {
        dao.upsertRsvp(EventRsvpDBO("e1", "u1", "going"))
        dao.upsertRsvp(EventRsvpDBO("e1", "u2", "not_going"))
        dao.upsertRsvp(EventRsvpDBO("e2", "u1", "going"))

        assertEquals(setOf("u1", "u2"), dao.getAttendees("e1").first().map { it.userId }.toSet())
    }

    @Test
    fun `an rsvp is replaced when the same user answers again`() = runTest {
        dao.upsertRsvp(EventRsvpDBO("e1", "u1", "going"))
        dao.upsertRsvp(EventRsvpDBO("e1", "u1", "not_going"))

        assertEquals("not_going", dao.observeMyRsvp("e1", "u1").first()?.status)
        assertNull(dao.observeMyRsvp("e1", "u9").first())
    }
}
