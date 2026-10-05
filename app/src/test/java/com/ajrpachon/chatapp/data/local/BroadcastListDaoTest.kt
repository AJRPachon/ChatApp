package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.BroadcastListDao
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.data.local.entity.UserDBO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BroadcastListDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: BroadcastListDao get() = db.broadcastListDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun user(id: String) = UserDBO(id = id, email = "", username = id, displayName = id, avatarUrl = null, createdAt = 0L)

    @Test
    fun `lists are ordered newest first`() = runTest {
        dao.insert(BroadcastListDBO("l1", "Vieja", createdAt = 1L))
        dao.insert(BroadcastListDBO("l2", "Nueva", createdAt = 2L))

        assertEquals(listOf("Nueva", "Vieja"), dao.observeAll().first().map { it.name })
        assertEquals(listOf("Nueva", "Vieja"), dao.getAll().map { it.name })
    }

    @Test
    fun `insertWithMembers stores the list and resolves its members as users`() = runTest {
        db.userDao().upsertAll(listOf(user("a"), user("b"), user("c")))

        dao.insertWithMembers(
            BroadcastListDBO("l1", "Familia", createdAt = 1L),
            listOf(BroadcastListMemberDBO("l1", "a"), BroadcastListMemberDBO("l1", "b")),
        )

        assertEquals(setOf("a", "b"), dao.getMembersForList("l1").map { it.id }.toSet())
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `insertWithMembers accepts a list without members`() = runTest {
        dao.insertWithMembers(BroadcastListDBO("l1", "Vacia", createdAt = 1L), emptyList())

        assertEquals(1, dao.getAll().size)
        assertTrue(dao.getMembersForList("l1").isEmpty())
    }

    @Test
    fun `deleteWithMembers removes the list and its members but not other lists`() = runTest {
        db.userDao().upsertAll(listOf(user("a"), user("b"), user("c")))
        dao.insertWithMembers(BroadcastListDBO("l1", "Una", 1L), listOf(BroadcastListMemberDBO("l1", "a")))
        dao.insertWithMembers(BroadcastListDBO("l2", "Otra", 2L), listOf(BroadcastListMemberDBO("l2", "a")))

        dao.deleteWithMembers("l1")

        assertEquals(listOf("l2"), dao.getAll().map { it.id })
        assertTrue(dao.getMembersForList("l1").isEmpty())
        assertEquals(listOf("a"), dao.getMembersForList("l2").map { it.id })
    }
}
