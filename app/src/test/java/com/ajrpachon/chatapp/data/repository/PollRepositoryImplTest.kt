package com.ajrpachon.chatapp.data.repository

import app.cash.turbine.test
import com.ajrpachon.chatapp.data.local.PollRepository as PollLocalDataSource
import com.ajrpachon.chatapp.data.local.dao.PollDao
import com.ajrpachon.chatapp.data.local.entity.PollDBO
import com.ajrpachon.chatapp.data.local.entity.PollOptionDBO
import com.ajrpachon.chatapp.data.local.entity.PollVoteDBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PollRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pollDao = mockk<PollDao>(relaxed = true)
    private val localDataSource = PollLocalDataSource(pollDao)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val repo = PollRepositoryImpl(localDataSource, analyticsTracker)

    // ── createPoll ───────────────────────────────────────────────────────────

    @Test
    fun `createPoll persists the poll and one option per entry with deterministic ids`() = runTest {
        val pollSlot = slot<PollDBO>()
        val optionsSlot = slot<List<PollOptionDBO>>()
        coEvery { pollDao.insertPoll(capture(pollSlot)) } returns Unit
        coEvery { pollDao.insertOptions(capture(optionsSlot)) } returns Unit

        val pollId = repo.createPoll(
            conversationId = "conv-1",
            question = "Pizza or sushi?",
            createdBy = "user-1",
            options = listOf("Pizza", "Sushi"),
            allowMultiple = true,
        )

        assertEquals(pollId, pollSlot.captured.id)
        assertEquals("conv-1", pollSlot.captured.conversationId)
        assertEquals("Pizza or sushi?", pollSlot.captured.question)
        assertEquals("user-1", pollSlot.captured.createdBy)
        assertTrue(pollSlot.captured.allowMultiple)

        assertEquals(2, optionsSlot.captured.size)
        assertEquals("$pollId-0", optionsSlot.captured[0].id)
        assertEquals("Pizza", optionsSlot.captured[0].text)
        assertEquals("$pollId-1", optionsSlot.captured[1].id)
        assertEquals("Sushi", optionsSlot.captured[1].text)
    }

    @Test
    fun `createPoll returns a non-blank generated poll id`() = runTest {
        val pollId = repo.createPoll("conv-1", "Q", "user-1", listOf("A"), false)

        assertTrue(pollId.isNotBlank())
    }

    // ── vote ─────────────────────────────────────────────────────────────────

    @Test
    fun `vote delegates to the local data source`() = runTest {
        repo.vote("poll-1", "user-1", "option-1")

        coVerify { pollDao.vote("poll-1", "user-1", "option-1") }
    }

    // ── observePollById ──────────────────────────────────────────────────────

    @Test
    fun `observePollById maps DBO to BO`() = runTest {
        every { pollDao.observePollById("poll-1") } returns flowOf(
            PollDBO(id = "poll-1", conversationId = "conv-1", question = "Q?", createdBy = "user-1", createdAt = 100L, allowMultiple = true)
        )

        repo.observePollById("poll-1").test {
            val bo = awaitItem()
            assertEquals("poll-1", bo?.id)
            assertEquals("conv-1", bo?.conversationId)
            assertEquals("Q?", bo?.question)
            assertEquals("user-1", bo?.createdBy)
            assertEquals(100L, bo?.createdAt)
            assertEquals(true, bo?.allowMultiple)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observePollById emits null when poll does not exist`() = runTest {
        every { pollDao.observePollById("missing") } returns flowOf(null)

        repo.observePollById("missing").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeOptionsByPollId ───────────────────────────────────────────────

    @Test
    fun `observeOptionsByPollId maps each option DBO to BO`() = runTest {
        every { pollDao.observeOptionsByPollId("poll-1") } returns flowOf(
            listOf(
                PollOptionDBO(id = "opt-1", pollId = "poll-1", text = "Pizza", voteCount = 3),
                PollOptionDBO(id = "opt-2", pollId = "poll-1", text = "Sushi", voteCount = 1),
            )
        )

        repo.observeOptionsByPollId("poll-1").test {
            val options = awaitItem()
            assertEquals(2, options.size)
            assertEquals("opt-1", options[0].id)
            assertEquals(3, options[0].voteCount)
            assertEquals("opt-2", options[1].id)
            assertEquals(1, options[1].voteCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeVotes ─────────────────────────────────────────────────────────

    @Test
    fun `observeVotes maps each vote DBO to BO`() = runTest {
        every { pollDao.observeVotes("poll-1", "user-1") } returns flowOf(
            listOf(PollVoteDBO(pollId = "poll-1", userId = "user-1", optionId = "opt-1"))
        )

        repo.observeVotes("poll-1", "user-1").test {
            val votes = awaitItem()
            assertEquals(1, votes.size)
            assertEquals("poll-1", votes[0].pollId)
            assertEquals("user-1", votes[0].userId)
            assertEquals("opt-1", votes[0].optionId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
