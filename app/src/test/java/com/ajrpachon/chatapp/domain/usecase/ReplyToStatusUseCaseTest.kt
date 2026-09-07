package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.StatusBO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyToStatusUseCaseTest {

    private val getOrCreateConversationUseCase = mockk<GetOrCreateConversationUseCase>()
    private val sendMessageUseCase = mockk<SendMessageUseCase>()
    private val useCase = ReplyToStatusUseCase(getOrCreateConversationUseCase, sendMessageUseCase)

    private val status = StatusBO(
        id = "status1",
        userId = "alice",
        userName = "Alice",
        userAvatarUrl = null,
        text = "hola a todos",
        imageUrl = null,
        videoUrl = null,
        backgroundColor = 0xFF1976D2,
        createdAt = Instant.fromEpochMilliseconds(1_000L),
        expiresAt = Instant.fromEpochMilliseconds(1_000L + 24 * 60 * 60 * 1000L),
        isFromMe = false,
    )

    private val fakeConversation = mockk<ConversationBO>(relaxed = true).also {
        io.mockk.every { it.id } returns "conv1"
    }
    private val fakeMessage = mockk<MessageBO>(relaxed = true)

    @Test
    fun `creates or gets the 1-1 conversation with the status owner`() = runTest {
        coEvery { getOrCreateConversationUseCase("me", "alice") } returns fakeConversation
        coEvery { sendMessageUseCase(any(), any(), any(), otherUserId = any(), statusReply = any()) } returns
            Result.success(fakeMessage)

        useCase("me", status, "qué bien!")

        coVerify { getOrCreateConversationUseCase("me", "alice") }
    }

    @Test
    fun `sends the reply text carrying a snapshot of the status`() = runTest {
        coEvery { getOrCreateConversationUseCase("me", "alice") } returns fakeConversation
        coEvery { sendMessageUseCase(any(), any(), any(), otherUserId = any(), statusReply = any()) } returns
            Result.success(fakeMessage)

        useCase("me", status, "qué bien!")

        coVerify {
            sendMessageUseCase(
                conversationId = "conv1",
                senderId = "me",
                content = "qué bien!",
                otherUserId = "alice",
                statusReply = match {
                    it.statusId == "status1" &&
                        it.statusOwnerId == "alice" &&
                        it.statusText == "hola a todos" &&
                        it.statusExpiresAt == status.expiresAt.toEpochMilliseconds()
                },
            )
        }
    }

    @Test
    fun `success returns the conversation id and status owner's display name`() = runTest {
        coEvery { getOrCreateConversationUseCase("me", "alice") } returns fakeConversation
        coEvery { sendMessageUseCase(any(), any(), any(), otherUserId = any(), statusReply = any()) } returns
            Result.success(fakeMessage)

        val result = useCase("me", status, "qué bien!").getOrThrow()

        assertEquals("conv1", result.conversationId)
        assertEquals("Alice", result.otherUserName)
    }

    @Test
    fun `propagates a failure from sendMessageUseCase`() = runTest {
        coEvery { getOrCreateConversationUseCase("me", "alice") } returns fakeConversation
        coEvery { sendMessageUseCase(any(), any(), any(), otherUserId = any(), statusReply = any()) } returns
            Result.failure(RuntimeException("network error"))

        val result = useCase("me", status, "qué bien!")

        assertTrue(result.isFailure)
        assertEquals("network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `propagates an exception from getOrCreateConversationUseCase`() = runTest {
        coEvery { getOrCreateConversationUseCase("me", "alice") } throws RuntimeException("db error")

        val result = useCase("me", status, "qué bien!")

        assertTrue(result.isFailure)
        assertEquals("db error", result.exceptionOrNull()?.message)
    }
}
