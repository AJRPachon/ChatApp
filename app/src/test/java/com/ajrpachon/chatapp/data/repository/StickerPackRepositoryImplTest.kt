package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.StickerPackDao
import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AnalyticsEvents
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StickerPackRepositoryImplTest {

    private val dao = mockk<StickerPackDao>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val repo = StickerPackRepositoryImpl(dao, analytics)

    @Test
    fun `installed and available packs are mapped from the matching dao queries`() = runTest {
        every { dao.getInstalledPacks() } returns flowOf(listOf(StickerPackDBO("p1", "Gatos", "c1", true)))
        every { dao.getAvailablePacks() } returns flowOf(listOf(StickerPackDBO("p2", "Perros", "c2", false)))

        assertEquals(listOf(StickerPackBO("p1", "Gatos", "c1", true)), repo.getInstalledPacks().first())
        assertEquals(listOf(StickerPackBO("p2", "Perros", "c2", false)), repo.getAvailablePacks().first())
    }

    @Test
    fun `getStickersForPack maps the stickers of that pack`() = runTest {
        every { dao.getStickersForPack("p1") } returns flowOf(listOf(StickerDBO("s1", "p1", "https://x/1.png", "feliz")))

        assertEquals(listOf(StickerBO("s1", "p1", "https://x/1.png", "feliz")), repo.getStickersForPack("p1").first())
    }

    @Test
    fun `installPack updates the dao and logs the install`() = runTest {
        repo.installPack("p1")

        coVerify { dao.installPack("p1") }
        coVerify { analytics.logEvent(AnalyticsEvents.STICKER_PACK_INSTALLED) }
    }
}
