package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ajrpachon.chatapp.data.local.dao.StickerPackDao
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
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
class StickerPackDaoTest {

    private lateinit var db: ChatDatabase
    private val dao: StickerPackDao get() = db.stickerPackDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun pack(id: String, name: String, installed: Boolean = false) =
        StickerPackDBO(id = id, name = name, coverUrl = "https://x/$id.png", isInstalled = installed)

    @Test
    fun `installed and available packs are split and sorted by name`() = runTest {
        dao.insertPacks(listOf(pack("p1", "Zorros", installed = true), pack("p2", "Gatos", installed = true), pack("p3", "Perros")))

        assertEquals(listOf("Gatos", "Zorros"), dao.getInstalledPacks().first().map { it.name })
        assertEquals(listOf("Perros"), dao.getAvailablePacks().first().map { it.name })
    }

    @Test
    fun `installPack moves a pack from available to installed`() = runTest {
        dao.insertPacks(listOf(pack("p1", "Gatos")))

        dao.installPack("p1")

        assertTrue(dao.getAvailablePacks().first().isEmpty())
        assertEquals(listOf("p1"), dao.getInstalledPacks().first().map { it.id })
    }

    @Test
    fun `inserting a pack again does not overwrite its installed flag`() = runTest {
        dao.insertPacks(listOf(pack("p1", "Gatos")))
        dao.installPack("p1")

        dao.insertPacks(listOf(pack("p1", "Gatos")))

        assertEquals(listOf("p1"), dao.getInstalledPacks().first().map { it.id })
    }

    @Test
    fun `stickers are returned for their own pack ordered by id`() = runTest {
        dao.insertPacks(listOf(pack("p1", "Gatos"), pack("p2", "Perros")))
        dao.insertStickers(
            listOf(
                StickerDBO("s2", "p1", "https://x/2.png"),
                StickerDBO("s1", "p1", "https://x/1.png"),
                StickerDBO("s3", "p2", "https://x/3.png"),
            ),
        )

        assertEquals(listOf("s1", "s2"), dao.getStickersForPack("p1").first().map { it.id })
        assertEquals(listOf("s3"), dao.getStickersForPack("p2").first().map { it.id })
    }
}
