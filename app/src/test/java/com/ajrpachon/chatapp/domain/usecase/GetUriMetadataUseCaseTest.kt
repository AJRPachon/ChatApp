package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.UriMetadata
import com.ajrpachon.chatapp.domain.repository.UriContentReader
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GetUriMetadataUseCaseTest {

    private val uriContentReader = mockk<UriContentReader>()
    private val useCase = GetUriMetadataUseCase(uriContentReader)

    @Test
    fun `delegates to repository and returns its metadata`() {
        val metadata = UriMetadata(mimeType = "image/png", displayName = "photo.png", size = 1024L)
        every { uriContentReader.getMetadata("content://photo") } returns metadata

        val result = useCase("content://photo")

        assertEquals(metadata, result)
    }
}
