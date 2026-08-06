package com.ajrpachon.chatapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GetCacheFileUseCaseTest {

    private val cacheDir = File("/tmp/cache")
    private val useCase = GetCacheFileUseCase(cacheDir)

    @Test
    fun `builds a File under the cache directory with the given filename`() {
        val result = useCase("photo.jpg")

        assertEquals(File(cacheDir, "photo.jpg"), result)
        assertEquals(cacheDir, result.parentFile)
    }

    @Test
    fun `preserves the exact filename including extension`() {
        val result = useCase("recording.m4a")

        assertEquals("recording.m4a", result.name)
    }
}
