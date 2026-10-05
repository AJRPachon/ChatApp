package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.FolderDao
import com.ajrpachon.chatapp.data.local.entity.FolderConversationDBO
import com.ajrpachon.chatapp.data.local.entity.FolderDBO
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
class FolderDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: FolderDao get() = db.folderDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `folders are ordered by sort order and then by name`() = runTest {
        dao.insert(FolderDBO("f1", "Trabajo", sortOrder = 1))
        dao.insert(FolderDBO("f2", "Amigos", sortOrder = 1))
        dao.insert(FolderDBO("f3", "Familia", sortOrder = 0))

        assertEquals(listOf("Familia", "Amigos", "Trabajo"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun `delete removes the folder`() = runTest {
        val folder = FolderDBO("f1", "Trabajo")
        dao.insert(folder)

        dao.delete(folder)

        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun `conversations can be added to and removed from a folder`() = runTest {
        dao.insert(FolderDBO("f1", "Trabajo"))
        dao.addConversation(FolderConversationDBO("f1", "c1"))
        dao.addConversation(FolderConversationDBO("f1", "c2"))
        dao.addConversation(FolderConversationDBO("f1", "c1"))

        assertEquals(setOf("c1", "c2"), dao.getConversationIds("f1").toSet())

        dao.removeConversation("f1", "c1")
        assertEquals(listOf("c2"), dao.getConversationIds("f1"))
    }

    @Test
    fun `clearFolder only empties that folder`() = runTest {
        dao.addConversation(FolderConversationDBO("f1", "c1"))
        dao.addConversation(FolderConversationDBO("f2", "c2"))

        dao.clearFolder("f1")

        assertTrue(dao.getConversationIds("f1").isEmpty())
        assertEquals(listOf("c2"), dao.getConversationIds("f2"))
    }
}
