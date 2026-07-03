package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class UserDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: UserDao get() = db.userDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── insertAndObserveCurrentUser ───────────────────────────────────────────

    @Test
    fun `insertAndObserveCurrentUser returns user with isCurrentUser true`() = runTest {
        dao.upsert(fakeUser("user1", isCurrentUser = true))

        val result = dao.observeCurrentUser().first()
        assertNotNull(result)
        assertEquals("user1", result!!.id)
    }

    @Test
    fun `observeCurrentUser returns null when no current user`() = runTest {
        dao.upsert(fakeUser("user1", isCurrentUser = false))

        val result = dao.observeCurrentUser().first()
        assertNull(result)
    }

    // ── clearCurrentUser ──────────────────────────────────────────────────────

    @Test
    fun `clearCurrentUser clears the isCurrentUser flag`() = runTest {
        dao.upsert(fakeUser("user1", isCurrentUser = true))
        dao.clearCurrentUser()

        val result = dao.observeCurrentUser().first()
        assertNull(result)
    }

    @Test
    fun `clearCurrentUser is no-op when no current user`() = runTest {
        dao.upsert(fakeUser("user1", isCurrentUser = false))
        dao.clearCurrentUser()

        val result = dao.observeCurrentUser().first()
        assertNull(result)
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun `getById returns correct user`() = runTest {
        dao.upsertAll(listOf(fakeUser("user1"), fakeUser("user2")))

        val result = dao.getById("user1")
        assertNotNull(result)
        assertEquals("user1", result!!.id)
    }

    @Test
    fun `getById returns null for unknown id`() = runTest {
        dao.upsert(fakeUser("user1"))

        val result = dao.getById("nonexistent")
        assertNull(result)
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    fun `upsert replaces existing user with same id`() = runTest {
        dao.upsert(fakeUser("user1", displayName = "Alice"))
        dao.upsert(fakeUser("user1", displayName = "Alice Updated"))

        val result = dao.getById("user1")
        assertNotNull(result)
        assertEquals("Alice Updated", result!!.displayName)
    }

    @Test
    fun `upsert inserts new user when id does not exist`() = runTest {
        dao.upsert(fakeUser("user1", displayName = "Bob"))

        val result = dao.getById("user1")
        assertNotNull(result)
        assertEquals("Bob", result!!.displayName)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun fakeUser(
        id: String,
        displayName: String = "Test User",
        isCurrentUser: Boolean = false,
    ) = UserDBO(
        id = id,
        email = "$id@example.com",
        username = id,
        displayName = displayName,
        avatarUrl = null,
        createdAt = System.currentTimeMillis(),
        isCurrentUser = isCurrentUser,
    )
}
