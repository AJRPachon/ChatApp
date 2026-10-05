package com.ajrpachon.chatapp.ui.broadcast

import com.ajrpachon.chatapp.domain.model.BroadcastListBO
import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.model.OutgoingMessageBO
import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.repository.BroadcastListRepository
import com.ajrpachon.chatapp.domain.usecase.GetCurrentUserUseCase
import com.ajrpachon.chatapp.domain.usecase.GetOrCreateConversationUseCase
import com.ajrpachon.chatapp.domain.usecase.SearchUsersUseCase
import com.ajrpachon.chatapp.domain.usecase.SendMessageUseCase
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BroadcastListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<BroadcastListRepository>(relaxed = true)
    private val getCurrentUser = mockk<GetCurrentUserUseCase>()
    private val searchUsers = mockk<SearchUsersUseCase>()
    private val getOrCreateConversation = mockk<GetOrCreateConversationUseCase>()
    private val sendMessage = mockk<SendMessageUseCase>()

    private val lists = MutableStateFlow<List<BroadcastListBO>>(emptyList())

    private fun user(id: String) = UserBO(
        id = id,
        email = "$id@test.com",
        username = id,
        displayName = "User $id",
        avatarUrl = null,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

    private fun conversation(id: String) = ConversationBO(
        id = id,
        name = "c",
        isGroup = false,
        participants = emptyList(),
        lastMessage = null,
        unreadCount = 0,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Before
    fun setUp() {
        every { getCurrentUser() } returns MutableStateFlow(user("me"))
        every { repository.observeAll() } returns lists
    }

    private fun buildViewModel() = BroadcastListViewModel(
        broadcastListRepository = repository,
        getCurrentUserUseCase = getCurrentUser,
        searchUsersUseCase = searchUsers,
        getOrCreateConversationUseCase = getOrCreateConversation,
        sendMessageUseCase = sendMessage,
    )

    @Test
    fun `lists from the repository are shown and loading ends`() = runTest(mainDispatcherRule.scheduler) {
        lists.value = listOf(BroadcastListBO("l1", "Familia", 1L, listOf(user("a"))))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals(listOf("Familia"), vm.state.value.lists.map { it.name })
        assertEquals(listOf("a"), vm.state.value.lists.single().members.map { it.id })
    }

    @Test
    fun `opening the create dialog resets the previous form`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(BroadcastListIntent.NameChanged("Viejo"))
        vm.onIntent(BroadcastListIntent.ToggleMember(user("a")))

        vm.onIntent(BroadcastListIntent.OpenCreateDialog)

        val state = vm.state.value
        assertTrue(state.showCreateDialog)
        assertEquals("", state.newListName)
        assertTrue(state.selectedMembers.isEmpty())
        assertTrue(state.selectedMemberIds.isEmpty())
    }

    @Test
    fun `toggling a member twice selects and then unselects it`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BroadcastListIntent.ToggleMember(user("a")))
        assertEquals(setOf("a"), vm.state.value.selectedMemberIds)

        vm.onIntent(BroadcastListIntent.ToggleMember(user("a")))
        assertTrue(vm.state.value.selectedMembers.isEmpty())
        assertTrue(vm.state.value.selectedMemberIds.isEmpty())
    }

    @Test
    fun `search results leave out the current user`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { searchUsers("an") } returns listOf(user("me"), user("ana"))
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BroadcastListIntent.SearchQueryChanged("an"))
        advanceUntilIdle()

        assertEquals(listOf("ana"), vm.state.value.searchResults.map { it.id })
    }

    @Test
    fun `a blank search clears the results without calling the use case`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BroadcastListIntent.SearchQueryChanged("  "))
        advanceUntilIdle()

        assertTrue(vm.state.value.searchResults.isEmpty())
        coVerify(exactly = 0) { searchUsers(any()) }
    }

    @Test
    fun `a failing search leaves the results empty`() = runTest(mainDispatcherRule.scheduler) {
        coEvery { searchUsers("an") } throws IllegalStateException("offline")
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(BroadcastListIntent.SearchQueryChanged("an"))
        advanceUntilIdle()

        assertTrue(vm.state.value.searchResults.isEmpty())
    }

    @Test
    fun `creating a list without a name or members sets an error and stores nothing`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()
            vm.onIntent(BroadcastListIntent.OpenCreateDialog)

            vm.onIntent(BroadcastListIntent.CreateList)
            advanceUntilIdle()

            assertNotNull(vm.state.value.error)
            coVerify(exactly = 0) { repository.create(any(), any(), any(), any()) }
        }

    @Test
    fun `creating a list stores it with the trimmed name and closes the dialog`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()
            vm.onIntent(BroadcastListIntent.OpenCreateDialog)
            vm.onIntent(BroadcastListIntent.NameChanged("  Amigos  "))
            vm.onIntent(BroadcastListIntent.ToggleMember(user("a")))
            vm.onIntent(BroadcastListIntent.ToggleMember(user("b")))
            val name = slot<String>()
            val members = slot<List<String>>()
            coEvery { repository.create(any(), capture(name), capture(members), any()) } returns Unit

            vm.onIntent(BroadcastListIntent.CreateList)
            advanceUntilIdle()

            assertEquals("Amigos", name.captured)
            assertEquals(listOf("a", "b"), members.captured)
            assertFalse(vm.state.value.showCreateDialog)
            assertFalse(vm.state.value.isCreating)
        }

    @Test
    fun `a failure while creating keeps the dialog open and shows the error`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()
            vm.onIntent(BroadcastListIntent.OpenCreateDialog)
            vm.onIntent(BroadcastListIntent.NameChanged("Amigos"))
            vm.onIntent(BroadcastListIntent.ToggleMember(user("a")))
            coEvery { repository.create(any(), any(), any(), any()) } throws IllegalStateException("db full")

            vm.onIntent(BroadcastListIntent.CreateList)
            advanceUntilIdle()

            assertEquals("db full", vm.state.value.error)
            assertTrue(vm.state.value.showCreateDialog)
            assertFalse(vm.state.value.isCreating)
        }

    @Test
    fun `deleting a list asks the repository and reports a failure as an error`() =
        runTest(mainDispatcherRule.scheduler) {
            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(BroadcastListIntent.DeleteList("l1"))
            advanceUntilIdle()
            coVerify { repository.delete("l1") }
            assertNull(vm.state.value.error)

            coEvery { repository.delete("l2") } throws IllegalStateException("locked")
            vm.onIntent(BroadcastListIntent.DeleteList("l2"))
            advanceUntilIdle()
            assertEquals("locked", vm.state.value.error)
        }

    @Test
    fun `dismissing the error clears it`() = runTest(mainDispatcherRule.scheduler) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(BroadcastListIntent.CreateList)
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        vm.onIntent(BroadcastListIntent.DismissError)

        assertNull(vm.state.value.error)
    }

    @Test
    fun `sending a blank message sets an error and sends nothing`() = runTest(mainDispatcherRule.scheduler) {
        lists.value = listOf(BroadcastListBO("l1", "Familia", 1L, listOf(user("a"))))
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(BroadcastListIntent.OpenSendDialog("l1"))
        vm.onIntent(BroadcastListIntent.BroadcastMessageChanged("   "))

        vm.onIntent(BroadcastListIntent.SendBroadcast)
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        coVerify(exactly = 0) { sendMessage(any()) }
    }

    @Test
    fun `broadcasting sends one message per member and reports how many went out`() =
        runTest(mainDispatcherRule.scheduler) {
            lists.value = listOf(BroadcastListBO("l1", "Familia", 1L, listOf(user("a"), user("b"), user("c"))))
            coEvery { getOrCreateConversation("me", any()) } answers { conversation("conv-${secondArg<String>()}") }
            val sent = mutableListOf<OutgoingMessageBO>()
            coEvery { sendMessage(capture(sent)) } answers {
                if (firstArg<OutgoingMessageBO>().otherUserId == "b") {
                    Result.failure(IllegalStateException("blocked"))
                } else {
                    Result.success(mockk<MessageBO>())
                }
            }
            val vm = buildViewModel()
            advanceUntilIdle()
            vm.onIntent(BroadcastListIntent.OpenSendDialog("l1"))
            vm.onIntent(BroadcastListIntent.BroadcastMessageChanged("  Hola a todos  "))

            vm.onIntent(BroadcastListIntent.SendBroadcast)
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c"), sent.map { it.otherUserId })
            assertTrue(sent.all { it.content == "Hola a todos" && it.senderId == "me" })
            assertEquals(listOf("conv-a", "conv-b", "conv-c"), sent.map { it.conversationId })
            assertFalse(vm.state.value.isSending)
            assertNull(vm.state.value.sendingListId)
            assertTrue(vm.effect.first() is BroadcastListEffect.ShowToast)
        }
}
