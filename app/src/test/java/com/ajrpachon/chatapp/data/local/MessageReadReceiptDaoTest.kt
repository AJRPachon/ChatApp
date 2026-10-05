package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.MessageReadReceiptDao
import com.ajrpachon.chatapp.data.local.entity.MessageReadReceiptDBO
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
class MessageReadReceiptDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: MessageReadReceiptDao get() = db.messageReadReceiptDao()

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
    fun `readers are listed per message`() = runTest {
        dao.insert(MessageReadReceiptDBO("m1", "u1", 10L))
        dao.insert(MessageReadReceiptDBO("m1", "u2", 20L))
        dao.insert(MessageReadReceiptDBO("m2", "u1", 30L))

        assertEquals(setOf("u1", "u2"), dao.getReadersForMessage("m1").first().map { it.userId }.toSet())
        assertEquals(listOf("u1"), dao.getReadersForMessage("m2").first().map { it.userId })
    }

    @Test
    fun `insert keeps the first receipt when the same reader is inserted twice`() = runTest {
        dao.insert(MessageReadReceiptDBO("m1", "u1", 10L))
        dao.insert(MessageReadReceiptDBO("m1", "u1", 99L))

        assertEquals(10L, dao.getReadersForMessage("m1").first().single().readAt)
    }

    @Test
    fun `markRead replaces an earlier receipt with the new time`() = runTest {
        dao.insert(MessageReadReceiptDBO("m1", "u1", 10L))

        dao.markRead("m1", "u1", 99L)

        assertEquals(99L, dao.getReadersForMessage("m1").first().single().readAt)
    }
}
