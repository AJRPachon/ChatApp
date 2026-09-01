package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.MessageDBO
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PendingMessageRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)

    private val repo = PendingMessageRepositoryImpl(messageDao, userDao)

    // ── getPendingMessages — fetches pending/failed and maps to domain ─────────

    @Test
    fun `getPendingMessages maps dao results to MessageBO`() = runTest {
        val dbos = listOf(
            fakeDbo("msg1", "conv1", "sender1", sendStatus = "pending"),
            fakeDbo("msg2", "conv1", "sender2", sendStatus = "failed"),
        )
        coEvery { messageDao.getPendingMessages() } returns dbos
        coEvery { userDao.getByIds(listOf("sender1", "sender2")) } returns emptyList()

        val result = repo.getPendingMessages()

        assertEquals(2, result.size)
        assertEquals("msg1", result[0].id)
        assertEquals("msg2", result[1].id)
    }

    // ── updateSendStatus — delegates to dao ──────────────────────────────────

    @Test
    fun `updateSendStatus delegates to dao`() = runTest {
        repo.updateSendStatus("msg1", "sent")

        coVerify { messageDao.updateSendStatus("msg1", "sent") }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeDbo(id: String, convId: String, senderId: String, sendStatus: String = "sent") = MessageDBO(
        id = id,
        conversationId = convId,
        senderId = senderId,
        content = "content",
        isRead = false,
        createdAt = 0L,
        sendStatus = sendStatus,
    )
}
