package com.ajrpachon.chatapp.domain.usecase

import app.cash.turbine.test
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveMessagesUseCaseTest {

    private val messageRepository = mockk<MessageRepository>()
    private val useCase = ObserveMessagesUseCase(messageRepository)

    @Test
    fun `delegates to repository with default historyVisibleFrom`() = runTest {
        val messages = listOf(mockk<MessageBO>(relaxed = true))
        every { messageRepository.observeMessages("conv-1", "user-1", 0L) } returns flowOf(messages)

        useCase("conv-1", "user-1").test {
            assertEquals(messages, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `passes explicit historyVisibleFrom through to repository`() = runTest {
        val messages = listOf(mockk<MessageBO>(relaxed = true))
        every { messageRepository.observeMessages("conv-1", "user-1", 500L) } returns flowOf(messages)

        useCase("conv-1", "user-1", 500L).test {
            assertEquals(messages, awaitItem())
            awaitComplete()
        }

        verify { messageRepository.observeMessages("conv-1", "user-1", 500L) }
    }
}
