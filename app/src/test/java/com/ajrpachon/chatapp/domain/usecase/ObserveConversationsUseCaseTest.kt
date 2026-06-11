package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveConversationsUseCaseTest {

    private val conversationRepository = mockk<ConversationRepository>()
    private val useCase = ObserveConversationsUseCase(conversationRepository)

    private val fakeConversation = mockk<ConversationBO>(relaxed = true)

    @Test
    fun `returns flow from repository`() = runTest {
        val flow = MutableStateFlow<List<ConversationBO>>(listOf(fakeConversation))
        every { conversationRepository.observeConversations("user1") } returns flow

        val result = useCase("user1").first()

        assertEquals(listOf(fakeConversation), result)
    }

    @Test
    fun `returns empty list when no conversations`() = runTest {
        val flow = MutableStateFlow<List<ConversationBO>>(emptyList())
        every { conversationRepository.observeConversations("user1") } returns flow

        val result = useCase("user1").first()

        assertEquals(emptyList<ConversationBO>(), result)
    }

    @Test
    fun `delegates to repository with correct userId`() = runTest {
        val flow = MutableStateFlow<List<ConversationBO>>(emptyList())
        every { conversationRepository.observeConversations(any()) } returns flow

        useCase("alice")

        verify { conversationRepository.observeConversations("alice") }
    }

    @Test
    fun `emits updated list when flow changes`() = runTest {
        val flow = MutableStateFlow<List<ConversationBO>>(emptyList())
        every { conversationRepository.observeConversations("user1") } returns flow

        val observedFlow = useCase("user1")
        assertEquals(emptyList<ConversationBO>(), observedFlow.first())

        flow.value = listOf(fakeConversation)
        assertEquals(listOf(fakeConversation), observedFlow.first())
    }
}
