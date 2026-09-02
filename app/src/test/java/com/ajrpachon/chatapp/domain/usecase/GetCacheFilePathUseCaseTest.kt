package com.ajrpachon.chatapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class GetCacheFilePathUseCaseTest {

    private val cacheDirPath = "/tmp/cache"
    private val useCase = GetCacheFilePathUseCase(cacheDirPath)

    @Test
    fun `builds a path under the cache directory with the given filename`() {
        val result = useCase("photo.jpg")

        assertEquals("$cacheDirPath/photo.jpg", result)
    }

    @Test
    fun `preserves the exact filename including extension`() {
        val result = useCase("recording.m4a")

        assertEquals(true, result.endsWith("recording.m4a"))
    }
}
