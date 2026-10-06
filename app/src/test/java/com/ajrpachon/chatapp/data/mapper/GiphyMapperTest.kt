package com.ajrpachon.chatapp.data.mapper

import com.ajrpachon.chatapp.data.remote.dto.GiphyGifDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyImageDataDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyImagesDTO
import com.ajrpachon.chatapp.domain.model.GiphyGifBO
import org.junit.Assert.assertEquals
import org.junit.Test

class GiphyMapperTest {

    @Test
    fun `gif uses the small fixed-height image as preview and the original as full`() {
        val dto = GiphyGifDTO(
            images = GiphyImagesDTO(
                fixedHeightSmall = GiphyImageDataDTO("https://g/small.gif"),
                original = GiphyImageDataDTO("https://g/original.gif"),
            ),
        )

        assertEquals(GiphyGifBO(previewUrl = "https://g/small.gif", fullUrl = "https://g/original.gif"), dto.toDomain())
    }
}
