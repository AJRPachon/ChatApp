package com.ajrpachon.chatapp.ui.chat.gallery

import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatMediaGalleryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val messageRepository = mockk<MessageRepository>(relaxed = true)

    private val conversationId = "conv1"

    private val imagesFlow = MutableStateFlow<List<String>>(emptyList())
    private val videosFlow = MutableStateFlow<List<String>>(emptyList())

    private fun buildViewModel(): ChatMediaGalleryViewModel {
        every { messageRepository.getImagesForConversation(conversationId) } returns imagesFlow
        every { messageRepository.getVideosForConversation(conversationId) } returns videosFlow
        return ChatMediaGalleryViewModel(
            conversationId = conversationId,
            messageRepository = messageRepository,
        )
    }

    @Test
    fun `initial state is empty before the repository emits`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        assertEquals(emptyList<String>(), vm.state.value.images)
        assertEquals(emptyList<String>(), vm.state.value.videos)
    }

    @Test
    fun `images are loaded from getImagesForConversation on init`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        imagesFlow.value = listOf("http://img1", "http://img2")
        advanceUntilIdle()
        assertEquals(listOf("http://img1", "http://img2"), vm.state.value.images)
    }

    @Test
    fun `videos are loaded from getVideosForConversation on init`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        videosFlow.value = listOf("http://video1")
        advanceUntilIdle()
        assertEquals(listOf("http://video1"), vm.state.value.videos)
    }

    @Test
    fun `Refresh intent re-collects both flows`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        imagesFlow.value = listOf("http://img1")
        videosFlow.value = listOf("http://video1")
        advanceUntilIdle()

        imagesFlow.value = listOf("http://img1", "http://img2")
        videosFlow.value = listOf("http://video1", "http://video2")
        vm.onIntent(ChatMediaGalleryIntent.Refresh)
        advanceUntilIdle()

        assertEquals(listOf("http://img1", "http://img2"), vm.state.value.images)
        assertEquals(listOf("http://video1", "http://video2"), vm.state.value.videos)
    }
}
