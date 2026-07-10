package com.ajrpachon.chatapp.data.repository

import app.cash.turbine.test
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.entity.ReactionDBO
import com.ajrpachon.chatapp.data.remote.source.ReactionRemoteSource
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReactionRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reactionDao = mockk<ReactionDao>(relaxed = true)
    private val remoteSource = mockk<ReactionRemoteSource>(relaxed = true)

    private val repo = ReactionRepositoryImpl(reactionDao, remoteSource)

    // ── observeReactions ──────────────────────────────────────────────────────

    @Test
    fun `observeReactions groups reactions by messageId`() = runTest {
        every { reactionDao.observeByConversation("conv1") } returns flowOf(
            listOf(
                ReactionDBO("msg1", "user1", "👍"),
                ReactionDBO("msg1", "user2", "👍"),
                ReactionDBO("msg2", "user1", "❤️"),
            )
        )

        repo.observeReactions("conv1").test {
            val map = awaitItem()
            assertEquals(2, map.size)
            assertEquals(2, map["msg1"]?.size)
            assertEquals(1, map["msg2"]?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeReactions maps DBO fields to ReactionBO correctly`() = runTest {
        every { reactionDao.observeByConversation("conv1") } returns flowOf(
            listOf(ReactionDBO("msg1", "user1", "🔥"))
        )

        repo.observeReactions("conv1").test {
            val map = awaitItem()
            val bo = map["msg1"]!!.first()
            assertEquals("msg1", bo.messageId)
            assertEquals("user1", bo.userId)
            assertEquals("🔥", bo.emoji)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeReactions returns empty map when no reactions`() = runTest {
        every { reactionDao.observeByConversation("conv1") } returns flowOf(emptyList())

        repo.observeReactions("conv1").test {
            val map = awaitItem()
            assertTrue(map.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── toggleReaction — reaction exists → delete ─────────────────────────────

    @Test
    fun `toggleReaction deletes reaction from Room when it already exists`() = runTest {
        coEvery { reactionDao.exists("msg1", "user1", "👍") } returns 1

        repo.toggleReaction("msg1", "user1", "👍")

        coVerify { reactionDao.delete("msg1", "user1", "👍") }
        coVerify(exactly = 0) { reactionDao.insert(any()) }
    }

    // ── toggleReaction — reaction does not exist → insert ────────────────────

    @Test
    fun `toggleReaction inserts reaction into Room when it does not exist`() = runTest {
        coEvery { reactionDao.exists("msg1", "user1", "👍") } returns 0

        repo.toggleReaction("msg1", "user1", "👍")

        coVerify { reactionDao.insert(ReactionDBO("msg1", "user1", "👍")) }
        coVerify(exactly = 0) { reactionDao.delete(any(), any(), any()) }
    }
}
