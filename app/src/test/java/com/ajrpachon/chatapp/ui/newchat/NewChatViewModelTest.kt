package com.ajrpachon.chatapp.ui.newchat

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.domain.usecase.BlockUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetDeviceContactsUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendInvitationResult
import com.ajrpachon.chatapp.domain.usecase.SendInvitationUseCase
import android.app.Application
import com.ajrpachon.chatapp.util.MainDispatcherRule
import com.ajrpachon.chatapp.util.sharedScheduler
import com.ajrpachon.chatapp.utils.ClipboardProtection
import com.ajrpachon.chatapp.utils.ContactSyncManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NewChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val searchUsersUseCase = mockk<SearchUsersUseCase>()
    private val sendInvitationUseCase = mockk<SendInvitationUseCase>()
    private val blockUserUseCase = mockk<BlockUserUseCase>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val contactSyncManager = mockk<ContactSyncManager>(relaxed = true)
    private val getDeviceContactsUseCase = mockk<GetDeviceContactsUseCase>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private val clipboardProtection = mockk<ClipboardProtection>(relaxed = true)

    private val testUser = UserBO(
        id = "user1",
        email = "user@test.com",
        username = "user1",
        displayName = "User One",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val otherUser = UserBO(
        id = "user2",
        email = "other@test.com",
        username = "user2",
        displayName = "User Two",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private val userFlow = MutableStateFlow<UserBO?>(testUser)

    @Before
    fun setUp() {
        every { getCurrentUserUseCase() } returns userFlow
        coEvery { searchUsersUseCase(any()) } returns emptyList()
        coEvery { sendInvitationUseCase.checkRelationship(any(), any()) } returns UserRelationship.NONE
        coEvery { contactSyncManager.readContactEmails() } returns emptyList()
    }

    private fun buildViewModel() = NewChatViewModel(
        application = application,
        clipboardProtection = clipboardProtection,
        getCurrentUserUseCase = getCurrentUserUseCase,
        searchUsersUseCase = searchUsersUseCase,
        sendInvitationUseCase = sendInvitationUseCase,
        blockUserUseCase = blockUserUseCase,
        userRepository = userRepository,
        contactSyncManager = contactSyncManager,
        getDeviceContactsUseCase = getDeviceContactsUseCase,
    )

    @Test
    fun `search returns empty list when no users found`() = runTest(sharedScheduler) {
        coEvery { searchUsersUseCase(any()) } returns emptyList()
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.QueryChanged("nobody"))
        advanceUntilIdle()
        assertTrue(vm.state.value.appUsers.isEmpty())
    }

    @Test
    fun `search returns users when found and filters out self`() = runTest(sharedScheduler) {
        coEvery { searchUsersUseCase("user") } returns listOf(testUser, otherUser)
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.QueryChanged("user"))
        advanceUntilIdle()
        // testUser (self) is filtered out; only otherUser appears
        assertEquals(listOf(otherUser), vm.state.value.appUsers)
    }

    @Test
    fun `QR scan of own userId shows ShowMessage effect`() = runTest(sharedScheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.UserScannedByQr("user1"))
        advanceUntilIdle()
        val effect = vm.effect.first()
        assertTrue(effect is NewChatEffect.ShowMessage)
        assertTrue((effect as NewChatEffect.ShowMessage).text.contains("propio"))
    }

    @Test
    fun `UserAction with BLOCKED relationship shows ShowMessage effect`() = runTest(sharedScheduler) {
        coEvery { sendInvitationUseCase(otherUser) } returns SendInvitationResult.Blocked
        val vm = buildViewModel()
        advanceUntilIdle()
        // Set relationship to NONE so the ViewModel calls sendInvitationUseCase
        vm.onIntent(NewChatIntent.UserAction(otherUser))
        advanceUntilIdle()
        val effect = vm.effect.first()
        assertTrue(effect is NewChatEffect.ShowMessage)
    }

    @Test
    fun `UserAction with NavigateToChat result emits NavigateToChat effect`() = runTest(sharedScheduler) {
        coEvery { sendInvitationUseCase(otherUser) } returns SendInvitationResult.NavigateToChat("conv1", "User Two")
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.UserAction(otherUser))
        advanceUntilIdle()
        val effect = vm.effect.first()
        assertTrue(effect is NewChatEffect.NavigateToChat)
        assertEquals("conv1", (effect as NewChatEffect.NavigateToChat).conversationId)
    }

    @Test
    fun `UserAction with PendingReceived result emits NavigateToInvitations effect`() = runTest(sharedScheduler) {
        coEvery { sendInvitationUseCase(otherUser) } returns SendInvitationResult.PendingReceived
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.UserAction(otherUser))
        advanceUntilIdle()
        val effect = vm.effect.first()
        assertTrue(effect is NewChatEffect.NavigateToInvitations)
    }

    @Test
    fun `UserAction with Sent result emits ShowMessage effect`() = runTest(sharedScheduler) {
        coEvery { sendInvitationUseCase(otherUser) } returns SendInvitationResult.Sent
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.UserAction(otherUser))
        advanceUntilIdle()
        val effect = vm.effect.first()
        assertTrue(effect is NewChatEffect.ShowMessage)
    }

    @Test
    fun `UserAction when relationship is PENDING_SENT emits ShowMessage without calling sendInvitation`() = runTest(sharedScheduler) {
        // Pre-seed relationship as PENDING_SENT via state
        coEvery { sendInvitationUseCase(otherUser) } returns SendInvitationResult.AlreadySent
        val vm = buildViewModel()
        advanceUntilIdle()
        // First call sets PENDING_SENT relationship
        vm.onIntent(NewChatIntent.UserAction(otherUser))
        advanceUntilIdle()
        // Ensure relationship is now PENDING_SENT
        assertEquals(UserRelationship.PENDING_SENT, vm.state.value.userRelationships[otherUser.id])
        // Second call: should short-circuit and emit ShowMessage (pending hint)
        vm.onIntent(NewChatIntent.UserAction(otherUser))
        advanceUntilIdle()
        val effect = vm.effect.first() // consume first effect
        val effect2 = vm.effect.first()
        assertTrue(effect2 is NewChatEffect.ShowMessage)
    }

    @Test
    fun `search failure updates error state`() = runTest(sharedScheduler) {
        coEvery { searchUsersUseCase("fail") } throws RuntimeException("network error")
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.QueryChanged("fail"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals("network error", vm.state.value.error)
    }

    @Test
    fun `DismissError clears the error`() = runTest(sharedScheduler) {
        coEvery { searchUsersUseCase("fail") } throws RuntimeException("oops")
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(NewChatIntent.QueryChanged("fail"))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        vm.onIntent(NewChatIntent.DismissError)
        assertNull(vm.state.value.error)
    }
}
