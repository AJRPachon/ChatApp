package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// sendMessage has 19 params — use anyArgs() to avoid fragile positional any() chains
private fun stubSend(repo: MessageRepository, result: MessageBO) {
    coEvery { repo.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns result
}

class SendMessageUseCaseTest {

    private val messageRepository = mockk<MessageRepository>()
    private val useCase = SendMessageUseCase(messageRepository)
    private val fakeMessage = mockk<MessageBO>(relaxed = true)

    @Test
    fun `returns success when repository sends message`() = runTest {
        stubSend(messageRepository, fakeMessage)

        val result = useCase("conv1", "user1", "Hello")

        assertTrue(result.isSuccess)
        assertEquals(fakeMessage, result.getOrNull())
    }

    @Test
    fun `trims whitespace from content before sending`() = runTest {
        stubSend(messageRepository, fakeMessage)

        useCase("conv1", "user1", "  Hello  ")

        coVerify {
            messageRepository.sendMessage(
                "conv1", "user1", "Hello",
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    fun `returns failure when content is blank and no media`() = runTest {
        val result = useCase("conv1", "user1", "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `succeeds with blank content when imageUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase("conv1", "user1", "", imageUrl = "https://img.jpg")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds with blank content when audioUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase("conv1", "user1", "", audioUrl = "https://audio.mp3")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds with blank content when gifUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase("conv1", "user1", "", gifUrl = "https://gif.gif")
        assertTrue(result.isSuccess)
    }

    // ── file sharing (feature batch1) ─────────────────────────────────────────

    @Test
    fun `succeeds with blank content when fileUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase("conv1", "user1", "", fileUrl = "https://storage/file.pdf")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes file metadata to repository`() = runTest {
        stubSend(messageRepository, fakeMessage)

        useCase(
            "conv1", "user1", "",
            fileUrl = "https://storage/file.pdf",
            fileName = "document.pdf",
            fileSize = 1024L,
            fileMimeType = "application/pdf",
        )

        coVerify {
            messageRepository.sendMessage(
                any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                fileUrl = "https://storage/file.pdf",
                fileName = "document.pdf",
                fileSize = 1024L,
                fileMimeType = "application/pdf",
                any(), any(),
            )
        }
    }

    // ── video messages (feature batch1) ───────────────────────────────────────

    @Test
    fun `succeeds with blank content when videoUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase("conv1", "user1", "", videoUrl = "https://storage/video.mp4")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes videoUrl to repository`() = runTest {
        stubSend(messageRepository, fakeMessage)

        useCase("conv1", "user1", "", videoUrl = "https://storage/video.mp4")

        coVerify {
            messageRepository.sendMessage(
                any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(),
                videoUrl = "https://storage/video.mp4",
                any(),
            )
        }
    }

    // ── blanks with all media null ─────────────────────────────────────────────

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery {
            messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("network error")

        val result = useCase("conv1", "user1", "Hello")

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }
}
