package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.OutgoingMessage
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun stubSend(repo: MessageRepository, result: MessageBO) {
    coEvery { repo.sendMessage(any()) } returns result
}

class SendMessageUseCaseTest {

    private val messageRepository = mockk<MessageRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val useCase = SendMessageUseCase(messageRepository, analyticsTracker)
    private val fakeMessage = mockk<MessageBO>(relaxed = true)

    private fun outgoing(content: String, build: OutgoingMessage.() -> OutgoingMessage = { this }) =
        OutgoingMessage(conversationId = "conv1", senderId = "user1", content = content).build()

    @Test
    fun `returns success when repository sends message`() = runTest {
        stubSend(messageRepository, fakeMessage)

        val result = useCase(outgoing("Hello"))

        assertTrue(result.isSuccess)
        assertEquals(fakeMessage, result.getOrNull())
    }

    @Test
    fun `trims whitespace from content before sending`() = runTest {
        stubSend(messageRepository, fakeMessage)

        useCase(outgoing("  Hello  "))

        coVerify {
            messageRepository.sendMessage(
                match { it.conversationId == "conv1" && it.senderId == "user1" && it.content == "Hello" },
            )
        }
    }

    @Test
    fun `returns failure when content is blank and no media`() = runTest {
        val result = useCase(outgoing("   "))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `succeeds with blank content when imageUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase(outgoing("") { copy(imageUrl = "https://img.jpg") })
        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds with blank content when audioUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase(outgoing("") { copy(audioUrl = "https://audio.mp3") })
        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds with blank content when gifUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase(outgoing("") { copy(gifUrl = "https://gif.gif") })
        assertTrue(result.isSuccess)
    }

    // ── file sharing (feature batch1) ─────────────────────────────────────────

    @Test
    fun `succeeds with blank content when fileUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase(outgoing("") { copy(fileUrl = "https://storage/file.pdf") })
        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes file metadata to repository`() = runTest {
        stubSend(messageRepository, fakeMessage)

        useCase(
            outgoing("") {
                copy(
                    fileUrl = "https://storage/file.pdf",
                    fileName = "document.pdf",
                    fileSize = 1024L,
                    fileMimeType = "application/pdf",
                )
            },
        )

        coVerify {
            messageRepository.sendMessage(
                match {
                    it.fileUrl == "https://storage/file.pdf" &&
                        it.fileName == "document.pdf" &&
                        it.fileSize == 1024L &&
                        it.fileMimeType == "application/pdf"
                },
            )
        }
    }

    // ── video messages (feature batch1) ───────────────────────────────────────

    @Test
    fun `succeeds with blank content when videoUrl is provided`() = runTest {
        stubSend(messageRepository, fakeMessage)
        val result = useCase(outgoing("") { copy(videoUrl = "https://storage/video.mp4") })
        assertTrue(result.isSuccess)
    }

    @Test
    fun `passes videoUrl to repository`() = runTest {
        stubSend(messageRepository, fakeMessage)

        useCase(outgoing("") { copy(videoUrl = "https://storage/video.mp4") })

        coVerify {
            messageRepository.sendMessage(match { it.videoUrl == "https://storage/video.mp4" })
        }
    }

    // ── repository failures ────────────────────────────────────────────────────

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { messageRepository.sendMessage(any()) } throws RuntimeException("network error")

        val result = useCase(outgoing("Hello"))

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }
}
