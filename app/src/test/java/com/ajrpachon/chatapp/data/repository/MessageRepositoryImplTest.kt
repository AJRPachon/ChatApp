package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.MessageDao
import com.ajrpachon.chatapp.data.local.dao.ReactionDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.remote.dto.MessageDTO
import com.ajrpachon.chatapp.data.remote.source.MessageRemoteSource
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.utils.UploadLimits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessageRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageDao = mockk<MessageDao>(relaxed = true)
    private val userDao = mockk<UserDao>(relaxed = true)
    private val reactionDao = mockk<ReactionDao>(relaxed = true)
    private val remoteSource = mockk<MessageRemoteSource>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val e2eeCoder = mockk<MessageE2EECoder>(relaxed = true)

    private val repo = MessageRepositoryImpl(
        messageDao, userDao, reactionDao, remoteSource, analyticsTracker, e2eeCoder,
    )

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

    // ── uploads — size check here, storage in the remote source ───────────────

    @Test
    fun `uploadImage returns the url from the remote source`() = runTest {
        val bytes = ByteArray(16)
        coEvery { remoteSource.uploadImage("conv1", bytes, "image/png") } returns "https://cdn/i.png"

        assertEquals("https://cdn/i.png", repo.uploadImage("conv1", bytes, "image/png"))
    }

    @Test
    fun `uploadAudio, uploadFile and uploadVideo pass their arguments to the remote source`() = runTest {
        val bytes = ByteArray(16)
        coEvery { remoteSource.uploadAudio("conv1", bytes) } returns "https://cdn/a.m4a"
        coEvery { remoteSource.uploadFile("conv1", bytes, "doc.pdf") } returns "https://cdn/doc.pdf"
        coEvery { remoteSource.uploadVideo("conv1", bytes) } returns "https://cdn/v.mp4"

        assertEquals("https://cdn/a.m4a", repo.uploadAudio("conv1", bytes))
        assertEquals("https://cdn/doc.pdf", repo.uploadFile("conv1", bytes, "doc.pdf", "application/pdf"))
        assertEquals("https://cdn/v.mp4", repo.uploadVideo("conv1", bytes))
    }

    @Test
    fun `uploadImage rejects an oversized image before touching the remote source`() = runTest {
        val tooBig = ByteArray((UploadLimits.IMAGE_MAX_BYTES + 1).toInt())

        val result = runCatching { repo.uploadImage("conv1", tooBig, "image/jpeg") }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { remoteSource.uploadImage(any(), any(), any()) }
    }

    private fun fakeDto(id: String, convId: String) = MessageDTO(
        id = id,
        conversationId = convId,
        senderId = "sender",
        content = "content",
        isRead = false,
        createdAt = "2026-01-01T00:00:00Z",
    )

}
