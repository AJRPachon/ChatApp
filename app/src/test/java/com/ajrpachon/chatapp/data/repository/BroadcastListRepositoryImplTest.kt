package com.ajrpachon.chatapp.data.repository

import app.cash.turbine.test
import com.ajrpachon.chatapp.data.local.dao.BroadcastListDao
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BroadcastListRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = mockk<BroadcastListDao>(relaxed = true)
    private val repo = BroadcastListRepositoryImpl(dao)

    // ── observeAll ───────────────────────────────────────────────────────────

    @Test
    fun `observeAll enriches each list with its members`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(BroadcastListDBO(id = "list-1", name = "Friends", createdAt = 100L))
        )
        coEvery { dao.getMembersForList("list-1") } returns listOf(user("user-1"), user("user-2"))

        repo.observeAll().test {
            val lists = awaitItem()
            assertEquals(1, lists.size)
            assertEquals("list-1", lists[0].id)
            assertEquals("Friends", lists[0].name)
            assertEquals(2, lists[0].members.size)
            assertEquals("user-1", lists[0].members[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll defaults members to empty list when the member query throws`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(BroadcastListDBO(id = "list-1", name = "Friends", createdAt = 100L))
        )
        coEvery { dao.getMembersForList("list-1") } throws RuntimeException("db error")

        repo.observeAll().test {
            val lists = awaitItem()
            assertTrue(lists[0].members.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAll emits an empty list when there are no broadcast lists`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())

        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    fun `create builds the list row and one member row per id`() = runTest {
        val listSlot = slot<BroadcastListDBO>()
        val membersSlot = slot<List<BroadcastListMemberDBO>>()
        coEvery { dao.insertWithMembers(capture(listSlot), capture(membersSlot)) } returns Unit

        repo.create(id = "list-1", name = "Family", memberIds = listOf("user-1", "user-2"), createdAt = 500L)

        assertEquals("list-1", listSlot.captured.id)
        assertEquals("Family", listSlot.captured.name)
        assertEquals(500L, listSlot.captured.createdAt)
        assertEquals(2, membersSlot.captured.size)
        assertEquals("user-1", membersSlot.captured[0].userId)
        assertEquals("list-1", membersSlot.captured[0].listId)
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    fun `delete delegates to dao`() = runTest {
        repo.delete("list-1")

        coVerify { dao.deleteWithMembers("list-1") }
    }

    private fun user(id: String) = UserDBO(
        id = id,
        email = "$id@example.com",
        username = id,
        displayName = id,
        avatarUrl = null,
        createdAt = 0L,
    )
}
