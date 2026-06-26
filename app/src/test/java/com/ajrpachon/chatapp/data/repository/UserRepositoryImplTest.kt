package com.ajrpachon.chatapp.data.repository

import app.cash.turbine.test
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import com.ajrpachon.chatapp.data.remote.dto.UserDTO
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class UserRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userDao = mockk<UserDao>(relaxed = true)
    private val remoteSource = mockk<UserRemoteSource>(relaxed = true)

    private val repo = UserRepositoryImpl(userDao, remoteSource)

    // ── getCurrentUserId ──────────────────────────────────────────────────────

    @Test
    fun `getCurrentUserId delegates to remote source`() {
        every { remoteSource.getCurrentUserId() } returns "uid123"

        assertEquals("uid123", repo.getCurrentUserId())
    }

    @Test
    fun `getCurrentUserId returns null when not logged in`() {
        every { remoteSource.getCurrentUserId() } returns null

        assertNull(repo.getCurrentUserId())
    }

    // ── getUserById — local cache hit ─────────────────────────────────────────

    @Test
    fun `getUserById returns cached user from dao`() = runTest {
        coEvery { userDao.getById("u1") } returns fakeUserDbo("u1", "Alice")

        val result = repo.getUserById("u1")

        assertNotNull(result)
        assertEquals("u1", result!!.id)
        assertEquals("Alice", result.displayName)
        coVerify(exactly = 0) { remoteSource.getProfile(any()) }
    }

    @Test
    fun `getUserById falls back to remote when not cached`() = runTest {
        coEvery { userDao.getById("u2") } returns null
        coEvery { remoteSource.getProfile("u2") } returns fakeUserDto("u2", "Bob")

        val result = repo.getUserById("u2")

        assertNotNull(result)
        assertEquals("Bob", result!!.displayName)
        coVerify { userDao.upsert(any()) }
    }

    @Test
    fun `getUserById returns null when not in local or remote`() = runTest {
        coEvery { userDao.getById(any()) } returns null
        coEvery { remoteSource.getProfile(any()) } returns null

        assertNull(repo.getUserById("ghost"))
    }

    // ── observeUserById ───────────────────────────────────────────────────────

    @Test
    fun `observeUserById emits mapped BO from Room`() = runTest {
        every { userDao.observeById("u1") } returns flowOf(fakeUserDbo("u1", "Alice"))

        repo.observeUserById("u1").test {
            val user = awaitItem()
            assertNotNull(user)
            assertEquals("Alice", user!!.displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeUserById emits null when user not present`() = runTest {
        every { userDao.observeById("ghost") } returns flowOf(null)

        repo.observeUserById("ghost").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── updateLastSeen ────────────────────────────────────────────────────────

    @Test
    fun `updateLastSeen calls remote and updates local`() = runTest {
        val dbo = fakeUserDbo("u1", "Alice")
        coEvery { userDao.getById("u1") } returns dbo

        repo.updateLastSeen("u1")

        coVerify { remoteSource.updateLastSeen("u1") }
        coVerify { userDao.upsert(match { it.id == "u1" && it.lastSeen != null }) }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeUserDbo(id: String, displayName: String) = UserDBO(
        id = id,
        email = "$id@test.com",
        username = displayName.lowercase(),
        displayName = displayName,
        avatarUrl = null,
        createdAt = 0L,
        isCurrentUser = false,
    )

    private fun fakeUserDto(id: String, displayName: String) = UserDTO(
        id = id,
        username = displayName.lowercase(),
        displayName = displayName,
        avatarUrl = null,
        createdAt = "2026-01-01T00:00:00Z",
    )
}
