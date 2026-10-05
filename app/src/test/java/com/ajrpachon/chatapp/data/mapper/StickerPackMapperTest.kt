package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.local.entity.StickerDBO
import com.ajrpachon.chatapp.data.local.entity.StickerPackDBO
import com.ajrpachon.chatapp.domain.model.StickerBO
import com.ajrpachon.chatapp.domain.model.StickerPackBO
import org.junit.Assert.assertEquals
import org.junit.Test

class StickerPackMapperTest {

    @Test
    fun `StickerPackDBO toBO maps name, cover and installed flag`() {
        val dbo = StickerPackDBO(id = "p1", name = "Gatos", coverUrl = "https://cdn/cover.png", isInstalled = true)

        assertEquals(StickerPackBO(id = "p1", name = "Gatos", coverUrl = "https://cdn/cover.png", isInstalled = true), dbo.toBO())
    }

    @Test
    fun `StickerPackDBO toBO keeps the not-installed default`() {
        assertEquals(false, StickerPackDBO(id = "p1", name = "Gatos", coverUrl = "c").toBO().isInstalled)
    }

    @Test
    fun `StickerDBO toBO maps pack, image and tags`() {
        val dbo = StickerDBO(id = "st1", packId = "p1", imageUrl = "https://cdn/s.png", tags = "gato,feliz")

        assertEquals(StickerBO(id = "st1", packId = "p1", imageUrl = "https://cdn/s.png", tags = "gato,feliz"), dbo.toBO())
    }
}
