package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.PollDao
import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
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
class PollDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: PollDao get() = db.pollDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun givenPoll(allowMultiple: Boolean) {
        dao.insertPoll(PollDBO("p1", "c1", "Cena?", "me", createdAt = 1L, allowMultiple = allowMultiple))
        dao.insertOptions(listOf(PollOptionDBO("o1", "p1", "Pizza"), PollOptionDBO("o2", "p1", "Sushi")))
    }

    private suspend fun counts() = dao.getOptions("p1").associate { it.id to it.voteCount }

    @Test
    fun `polls are listed per conversation newest first`() = runTest {
        dao.insertPoll(PollDBO("p1", "c1", "Vieja", "me", createdAt = 1L))
        dao.insertPoll(PollDBO("p2", "c1", "Nueva", "me", createdAt = 2L))
        dao.insertPoll(PollDBO("p3", "c2", "Otra conversacion", "me", createdAt = 3L))

        assertEquals(listOf("p2", "p1"), dao.observePollsByConversation("c1").first().map { it.id })
    }

    @Test
    fun `a poll and its options can be read back`() = runTest {
        givenPoll(allowMultiple = false)

        assertEquals("Cena?", dao.getPoll("p1")?.question)
        assertNull(dao.getPoll("nope"))
        assertEquals(setOf("o1", "o2"), dao.getOptions("p1").map { it.id }.toSet())
        assertEquals("p1", dao.observePollById("p1").first()?.id)
        assertEquals(2, dao.observeOptionsByPollId("p1").first().size)
    }

    @Test
    fun `voting for an option records the vote and increments its count`() = runTest {
        givenPoll(allowMultiple = false)

        dao.vote("p1", "u1", "o1")

        assertEquals(mapOf("o1" to 1, "o2" to 0), counts())
        assertEquals(listOf("o1"), dao.getVotes("p1", "u1").map { it.optionId })
    }

    @Test
    fun `single choice moves the vote to the new option`() = runTest {
        givenPoll(allowMultiple = false)
        dao.vote("p1", "u1", "o1")

        dao.vote("p1", "u1", "o2")

        assertEquals(mapOf("o1" to 0, "o2" to 1), counts())
        assertEquals(listOf("o2"), dao.getVotes("p1", "u1").map { it.optionId })
    }

    @Test
    fun `single choice un-votes when the selected option is tapped again`() = runTest {
        givenPoll(allowMultiple = false)
        dao.vote("p1", "u1", "o1")

        dao.vote("p1", "u1", "o1")

        assertEquals(mapOf("o1" to 0, "o2" to 0), counts())
        assertTrue(dao.getVotes("p1", "u1").isEmpty())
    }

    @Test
    fun `multiple choice keeps independent selections and toggles each one`() = runTest {
        givenPoll(allowMultiple = true)

        dao.vote("p1", "u1", "o1")
        dao.vote("p1", "u1", "o2")
        assertEquals(mapOf("o1" to 1, "o2" to 1), counts())

        dao.vote("p1", "u1", "o1")
        assertEquals(mapOf("o1" to 0, "o2" to 1), counts())
        assertEquals(listOf("o2"), dao.observeVotes("p1", "u1").first().map { it.optionId })
    }

    @Test
    fun `votes from different users add up`() = runTest {
        givenPoll(allowMultiple = false)

        dao.vote("p1", "u1", "o1")
        dao.vote("p1", "u2", "o1")

        assertEquals(2, counts().getValue("o1"))
    }

    @Test
    fun `voting on a poll that does not exist changes nothing`() = runTest {
        givenPoll(allowMultiple = false)

        dao.vote("missing", "u1", "o1")

        assertEquals(mapOf("o1" to 0, "o2" to 0), counts())
    }

    @Test
    fun `a vote count never goes below zero`() = runTest {
        givenPoll(allowMultiple = false)

        dao.decrementVoteCount("o1")

        assertEquals(0, counts().getValue("o1"))
    }
}
