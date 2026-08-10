package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.UriContentReader
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ReadUriAsBytesUseCaseTest {

    private val uriContentReader = mockk<UriContentReader>()
    private val useCase = ReadUriAsBytesUseCase(uriContentReader)

    @Test
    fun `delegates to repository and returns its bytes`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        coEvery { uriContentReader.readBytes("content://file") } returns bytes

        val result = useCase("content://file")

        assertArrayEquals(bytes, result)
    }
}
