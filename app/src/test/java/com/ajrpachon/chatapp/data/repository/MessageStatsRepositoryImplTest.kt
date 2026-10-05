package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.ConversationMessageCount
import com.ajrpachon.chatapp.data.local.dao.DayMessageCount
import com.ajrpachon.chatapp.data.local.dao.MessageDao
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageStatsRepositoryImplTest {

    private val dao = mockk<MessageDao>()
    private val repo = MessageStatsRepositoryImpl(dao)

    @Test
    fun `counters are read straight from the dao`() = runTest {
        coEvery { dao.countSent("me") } returns 4
        coEvery { dao.countReceived("me") } returns 7
        coEvery { dao.countCalls() } returns 2
        coEvery { dao.sumCallDurationSeconds() } returns 90
        coEvery { dao.countImages() } returns 3
        coEvery { dao.countAudio() } returns 5
        coEvery { dao.countVideos() } returns 1

        assertEquals(4, repo.countSent("me"))
        assertEquals(7, repo.countReceived("me"))
        assertEquals(2, repo.countCalls())
        assertEquals(90, repo.sumCallDurationSeconds())
        assertEquals(3, repo.countImages())
        assertEquals(5, repo.countAudio())
        assertEquals(1, repo.countVideos())
    }

    @Test
    fun `most active conversation is its id or null when there are no messages`() = runTest {
        coEvery { dao.getMostActiveConversation() } returns ConversationMessageCount("c1", 12)
        assertEquals("c1", repo.getMostActiveConversationId())

        coEvery { dao.getMostActiveConversation() } returns null
        assertNull(repo.getMostActiveConversationId())
    }

    @Test
    fun `messages per day become day and count pairs in dao order`() = runTest {
        coEvery { dao.countMessagesByDay(1_000L) } returns listOf(DayMessageCount(20_000L, 3), DayMessageCount(20_001L, 8))

        assertEquals(listOf(20_000L to 3, 20_001L to 8), repo.countMessagesByDay(1_000L))
    }
}
