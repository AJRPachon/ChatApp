package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MessageRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val remoteSource = mockk<MessageRemoteSource>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)

    private val repo = MessageRepositoryImpl(messageDao, userDao, remoteSource, supabase)

    // ── syncMessages — fetches remote and upserts ─────────────────────────────

    @Test
    fun `syncMessages fetches remote and upserts all to local`() = runTest {
        val dtos = listOf(
            fakeDto("msg1", "conv1"),
            fakeDto("msg2", "conv1"),
        )
        coEvery { remoteSource.getMessages("conv1", 0L) } returns dtos

        repo.syncMessages("conv1", 0L)

        coVerify { messageDao.upsertAll(match { it.size == 2 && it.any { dbo -> dbo.id == "msg1" } }) }
    }

    // ── markAsRead — delegates to dao and remote ─────────────────────────────

    @Test
    fun `markAsRead marks all local and calls remote`() = runTest {
        repo.markAsRead("conv1", "user1")

        coVerify { messageDao.markAllRead("conv1") }
        coVerify { remoteSource.markAsRead("conv1", "user1") }
    }

    // ── clearMessages ─────────────────────────────────────────────────────────

    @Test
    fun `clearMessages deletes all messages for conversation`() = runTest {
        repo.clearMessages("conv1")

        coVerify { messageDao.deleteByConversation("conv1") }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeDto(id: String, convId: String) = MessageDTO(
        id = id,
        conversationId = convId,
        senderId = "sender",
        content = "content",
        isRead = false,
        createdAt = "2026-01-01T00:00:00Z",
    )

}
