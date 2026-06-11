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

class SendMessageUseCaseTest {

    private val messageRepository = mockk<MessageRepository>()
    private val useCase = SendMessageUseCase(messageRepository)

    private val fakeMessage = mockk<MessageBO>(relaxed = true)

    @Test
    fun `returns success when repository sends message`() = runTest {
        coEvery { messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns fakeMessage

        val result = useCase("conv1", "user1", "Hello")

        assertTrue(result.isSuccess)
        assertEquals(fakeMessage, result.getOrNull())
    }

    @Test
    fun `trims whitespace from content before sending`() = runTest {
        coEvery { messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns fakeMessage

        useCase("conv1", "user1", "  Hello  ")

        coVerify { messageRepository.sendMessage("conv1", "user1", "Hello", any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `returns failure when content is blank and no media`() = runTest {
        val result = useCase("conv1", "user1", "   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `succeeds with blank content when imageUrl is provided`() = runTest {
        coEvery { messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns fakeMessage

        val result = useCase("conv1", "user1", "", imageUrl = "https://img.jpg")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds with blank content when audioUrl is provided`() = runTest {
        coEvery { messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns fakeMessage

        val result = useCase("conv1", "user1", "", audioUrl = "https://audio.mp3")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `succeeds with blank content when gifUrl is provided`() = runTest {
        coEvery { messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns fakeMessage

        val result = useCase("conv1", "user1", "", gifUrl = "https://gif.gif")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when repository throws`() = runTest {
        coEvery { messageRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("network error")

        val result = useCase("conv1", "user1", "Hello")

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }
}
