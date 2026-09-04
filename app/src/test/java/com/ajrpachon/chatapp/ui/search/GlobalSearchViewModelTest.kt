package com.ajrpachon.chatapp.ui.search

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GlobalSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository = mockk<MessageRepository>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private fun buildViewModel() =
        GlobalSearchViewModel(messageRepository, conversationRepository, analyticsTracker)

    private fun message(id: String, conversationId: String, content: String) = MessageBO(
        id = id,
        conversationId = conversationId,
        senderId = "user-1",
        senderName = "Alice",
        content = content,
        isRead = true,
        isFromMe = false,
        createdAt = Instant.fromEpochMilliseconds(1000L),
    )

    @Test
    fun `short query does not trigger a search and clears results`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GlobalSearchIntent.QueryChanged("a"))
        advanceUntilIdle()

        assertEquals("a", vm.state.value.query)
        assertTrue(vm.state.value.results.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `query with two or more characters searches and maps results`() = runTest(sharedScheduler) {
        coEvery { messageRepository.searchAllMessages("hello") } returns listOf(message("msg-1", "conv-1", "hello world"))
        coEvery { conversationRepository.getById("conv-1") } returns ConversationBO(
            id = "conv-1",
            name = "Team Chat",
            isGroup = true,
            participants = emptyList(),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GlobalSearchIntent.QueryChanged("hello"))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.results.size)
        assertEquals("msg-1", vm.state.value.results[0].messageId)
        assertEquals("Team Chat", vm.state.value.results[0].conversationName)
        assertEquals("hello world", vm.state.value.results[0].content)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `falls back to Chat as conversation name when conversation lookup returns null`() = runTest(sharedScheduler) {
        coEvery { messageRepository.searchAllMessages("hello") } returns listOf(message("msg-1", "conv-missing", "hi"))
        coEvery { conversationRepository.getById("conv-missing") } returns null

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GlobalSearchIntent.QueryChanged("hello"))
        advanceUntilIdle()

        assertEquals("Chat", vm.state.value.results[0].conversationName)
    }

    @Test
    fun `returns empty results when no messages match`() = runTest(sharedScheduler) {
        coEvery { messageRepository.searchAllMessages("nomatch") } returns emptyList()

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GlobalSearchIntent.QueryChanged("nomatch"))
        advanceUntilIdle()

        assertTrue(vm.state.value.results.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `only the latest query's results are kept when query changes quickly`() = runTest(sharedScheduler) {
        coEvery { messageRepository.searchAllMessages("first") } returns listOf(message("msg-first", "conv-1", "first"))
        coEvery { messageRepository.searchAllMessages("second") } returns listOf(message("msg-second", "conv-1", "second"))
        coEvery { conversationRepository.getById("conv-1") } returns null

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(GlobalSearchIntent.QueryChanged("first"))
        vm.onIntent(GlobalSearchIntent.QueryChanged("second"))
        advanceUntilIdle()

        assertEquals("second", vm.state.value.query)
        assertEquals(1, vm.state.value.results.size)
        assertEquals("msg-second", vm.state.value.results[0].messageId)
    }
}
