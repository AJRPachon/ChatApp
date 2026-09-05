package com.ajrpachon.chatapp.ui.userinfo

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.MessageRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UserInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepository = mockk<UserRepository>()
    private val conversationRepository = mockk<ConversationRepository>()
    private val messageRepository = mockk<MessageRepository>()

    private val otherUser = UserBO(
        id = "other-1",
        email = "other@test.com",
        username = "other_user",
        displayName = "Other User",
        avatarUrl = "avatar.png",
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val conversation = ConversationBO(
        id = "conv-1",
        name = "Other User",
        isGroup = false,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun message(imageUrl: String? = null, videoUrl: String? = null) = MessageBO(
        id = "msg-${imageUrl ?: videoUrl}",
        conversationId = "conv-1",
        senderId = "other-1",
        senderName = "Other User",
        content = "",
        isRead = true,
        isFromMe = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        imageUrl = imageUrl,
        videoUrl = videoUrl,
    )

    private fun buildViewModel() = UserInfoViewModel(
        userId = "other-1",
        userRepository = userRepository,
        conversationRepository = conversationRepository,
        messageRepository = messageRepository,
    )

    @Test
    fun `loads user profile fields on init`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { userRepository.getUserById("other-1") } returns otherUser
        every { userRepository.getCurrentUserId() } returns "current-user"
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user", "other-1") } returns conversation
        every { messageRepository.observeMessages("conv-1", "current-user") } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("Other User", vm.state.value.displayName)
        assertEquals("other_user", vm.state.value.username)
        assertEquals("avatar.png", vm.state.value.avatarUrl)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `falls back to blank fields when user is not found`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { userRepository.getUserById("other-1") } returns null
        every { userRepository.getCurrentUserId() } returns null

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("", vm.state.value.displayName)
        assertEquals("", vm.state.value.username)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `does not load media when there is no active session`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { userRepository.getUserById("other-1") } returns otherUser
        every { userRepository.getCurrentUserId() } returns null

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.mediaUrls.isEmpty())
    }

    @Test
    fun `collects distinct image and video urls from shared messages, most recent first`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { userRepository.getUserById("other-1") } returns otherUser
        every { userRepository.getCurrentUserId() } returns "current-user"
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user", "other-1") } returns conversation
        every { messageRepository.observeMessages("conv-1", "current-user") } returns flowOf(
            listOf(
                message(imageUrl = "img1.png"),
                message(videoUrl = "vid1.mp4"),
                message(imageUrl = "img1.png"), // duplicate, should be de-duplicated
            )
        )

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(listOf("vid1.mp4", "img1.png"), vm.state.value.mediaUrls)
    }

    @Test
    fun `Refresh intent reloads profile and media`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { userRepository.getUserById("other-1") } returns otherUser
        every { userRepository.getCurrentUserId() } returns "current-user"
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user", "other-1") } returns conversation
        every { messageRepository.observeMessages("conv-1", "current-user") } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(UserInfoIntent.Refresh)
        advanceUntilIdle()

        assertEquals("Other User", vm.state.value.displayName)
    }

    @Test
    fun `does not crash when conversation lookup fails`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { userRepository.getUserById("other-1") } returns otherUser
        every { userRepository.getCurrentUserId() } returns "current-user"
        coEvery { conversationRepository.getOrCreateDirectConversation("current-user", "other-1") } throws RuntimeException("boom")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.mediaUrls.isEmpty())
    }
}
