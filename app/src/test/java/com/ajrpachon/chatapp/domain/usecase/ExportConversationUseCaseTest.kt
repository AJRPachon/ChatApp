package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.SendStatus
import com.ajrpachon.chatapp.domain.repository.ConversationFileExporter
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportConversationUseCaseTest {

    private val messageRepository = mockk<MessageRepository>()
    private val conversationFileExporter = mockk<ConversationFileExporter>()
    private val useCase = ExportConversationUseCase(messageRepository, conversationFileExporter)

    private fun message(
        content: String = "",
        imageUrl: String? = null,
        audioUrl: String? = null,
        gifUrl: String? = null,
        stickerUrl: String? = null,
        fileUrl: String? = null,
        fileName: String? = null,
        videoUrl: String? = null,
        isDeleted: Boolean = false,
        senderName: String = "Alice",
    ) = MessageBO(
        id = "msg-id",
        conversationId = "conv-1",
        senderId = "user-1",
        senderName = senderName,
        content = content,
        isRead = true,
        isFromMe = false,
        createdAt = Instant.fromEpochMilliseconds(0L),
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        gifUrl = gifUrl,
        stickerUrl = stickerUrl,
        fileUrl = fileUrl,
        fileName = fileName,
        videoUrl = videoUrl,
        isDeleted = isDeleted,
        sendStatus = SendStatus.SENT,
    )

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { messageRepository.getAllMessages("conv-1", "user-1") } throws RuntimeException("db error")

        val result = useCase("conv-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals("db error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `writes and shares a formatted text file with the content of every message type`() = runTest {
        val messages = listOf(
            message(content = "Hello there"),
            message(imageUrl = "img.png"),
            message(audioUrl = "audio.mp3"),
            message(gifUrl = "gif.gif"),
            message(stickerUrl = "sticker.png"),
            message(fileUrl = "doc.pdf", fileName = "doc.pdf"),
            message(videoUrl = "video.mp4"),
        )
        coEvery { messageRepository.getAllMessages("conv-1", "user-1") } returns messages
        val contentSlot = slot<String>()
        coEvery { conversationFileExporter.writeAndShare("chat_conv-1.txt", capture(contentSlot)) } returns "content://export/chat_conv-1.txt"

        val result = useCase("conv-1", "user-1")

        assertTrue(result.isSuccess)
        assertEquals("content://export/chat_conv-1.txt", result.getOrNull())
        val text = contentSlot.captured
        assertTrue(text.contains("Alice: Hello there"))
        assertTrue(text.contains("[Imagen]"))
        assertTrue(text.contains("[Audio]"))
        assertTrue(text.contains("[GIF]"))
        assertTrue(text.contains("[Sticker]"))
        assertTrue(text.contains("[Archivo: doc.pdf]"))
        assertTrue(text.contains("[Video]"))
    }

    @Test
    fun `skips deleted messages when formatting`() = runTest {
        val messages = listOf(
            message(content = "kept message"),
            message(content = "deleted message", isDeleted = true),
        )
        coEvery { messageRepository.getAllMessages("conv-1", "user-1") } returns messages
        val contentSlot = slot<String>()
        coEvery { conversationFileExporter.writeAndShare(any(), capture(contentSlot)) } returns "uri"

        useCase("conv-1", "user-1")

        assertTrue(contentSlot.captured.contains("kept message"))
        assertTrue(!contentSlot.captured.contains("deleted message"))
    }

    @Test
    fun `skips messages with no renderable content`() = runTest {
        val messages = listOf(message(content = ""))
        coEvery { messageRepository.getAllMessages("conv-1", "user-1") } returns messages
        val contentSlot = slot<String>()
        coEvery { conversationFileExporter.writeAndShare(any(), capture(contentSlot)) } returns "uri"

        useCase("conv-1", "user-1")

        assertEquals("", contentSlot.captured)
    }

    @Test
    fun `returns failure when exporter throws`() = runTest {
        coEvery { messageRepository.getAllMessages("conv-1", "user-1") } returns emptyList()
        coEvery { conversationFileExporter.writeAndShare(any(), any()) } throws RuntimeException("write failed")

        val result = useCase("conv-1", "user-1")

        assertTrue(result.isFailure)
        assertEquals("write failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `passes the expected file name to the exporter`() = runTest {
        coEvery { messageRepository.getAllMessages("conv-42", "user-1") } returns emptyList()
        coEvery { conversationFileExporter.writeAndShare("chat_conv-42.txt", any()) } returns "uri"

        useCase("conv-42", "user-1")

        coVerify { conversationFileExporter.writeAndShare("chat_conv-42.txt", any()) }
    }
}
