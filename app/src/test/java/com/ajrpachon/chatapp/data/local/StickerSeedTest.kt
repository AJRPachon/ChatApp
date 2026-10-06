package com.ajrpachon.chatapp.data.local

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class StickerSeedTest {

    private lateinit var db: ChatDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().addCallback(stickerSeedCallback).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a new database has the four built-in packs`() = runTest {
        val installed = db.stickerPackDao().getInstalledPacks().first().map { it.id }
        val available = db.stickerPackDao().getAvailablePacks().first().map { it.id }

        assertEquals(setOf("pack_animals", "pack_faces", "pack_party"), installed.toSet())
        assertEquals(listOf("pack_travel"), available)
    }

    @Test
    fun `each pack comes with ten stickers that point at its own pack`() = runTest {
        listOf("pack_animals", "pack_faces", "pack_party", "pack_travel").forEach { packId ->
            val stickers = db.stickerPackDao().getStickersForPack(packId).first()

            assertEquals(packId, 10, stickers.size)
            assertTrue(packId, stickers.all { it.packId == packId && it.imageUrl.endsWith(".png") })
        }
    }
}
