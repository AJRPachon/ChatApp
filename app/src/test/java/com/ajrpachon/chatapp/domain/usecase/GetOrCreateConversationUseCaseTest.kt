package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetOrCreateConversationUseCaseTest {

    private val conversationRepository = mockk<ConversationRepository>()
    private val useCase = GetOrCreateConversationUseCase(conversationRepository)

    private val fakeConversation = mockk<ConversationBO>(relaxed = true)

    @Test
    fun `returns conversation from repository`() = runTest {
        coEvery { conversationRepository.getOrCreateDirectConversation("user1", "user2") } returns fakeConversation

        val result = useCase("user1", "user2")

        assertEquals(fakeConversation, result)
    }

    @Test
    fun `delegates to repository with correct user ids`() = runTest {
        coEvery { conversationRepository.getOrCreateDirectConversation(any(), any()) } returns fakeConversation

        useCase("alice", "bob")

        coVerify { conversationRepository.getOrCreateDirectConversation("alice", "bob") }
    }

    @Test
    fun `propagates exception from repository`() = runTest {
        coEvery { conversationRepository.getOrCreateDirectConversation(any(), any()) } throws RuntimeException("DB error")

        val result = runCatching { useCase("user1", "user2") }

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
