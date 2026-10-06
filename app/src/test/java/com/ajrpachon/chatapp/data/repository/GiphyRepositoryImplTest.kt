package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.dto.GiphyGifDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyImageDataDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyImagesDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyMetaDTO
import com.ajrpachon.chatapp.data.remote.dto.GiphyResponseDTO
import com.ajrpachon.chatapp.data.remote.source.GiphyRemoteSource
import com.ajrpachon.chatapp.domain.model.GiphyGifBO
import com.ajrpachon.chatapp.domain.model.GiphySearchResult
import com.ajrpachon.chatapp.util.FakeSecureStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GiphyRepositoryImplTest {

    private val remote = mockk<GiphyRemoteSource>()
    private val storage = FakeSecureStorage()

    private fun repo(defaultApiKey: String = "") =
        GiphyRepositoryImpl(remote, storageProvider = { storage }, defaultApiKey = defaultApiKey)

    private fun response(status: Int, vararg gifs: GiphyGifDTO) =
        GiphyResponseDTO(data = gifs.toList(), meta = GiphyMetaDTO(status = status))

    private fun gif(small: String, original: String) = GiphyGifDTO(
        GiphyImagesDTO(fixedHeightSmall = GiphyImageDataDTO(small), original = GiphyImageDataDTO(original)),
    )

    @Test
    fun `search without any api key does not call the network`() = runTest {
        val result = repo(defaultApiKey = "").search("gatos")

        assertEquals(GiphySearchResult.ApiKeyInvalid, result)
        coVerify(exactly = 0) { remote.search(any(), any()) }
    }

    @Test
    fun `a stored key is used before the default one`() = runTest {
        storage.putString("giphy_api_key", "stored")
        coEvery { remote.search("stored", "gatos") } returns response(200)

        repo(defaultApiKey = "default").search("gatos")

        coVerify { remote.search("stored", "gatos") }
        coVerify(exactly = 0) { remote.search("default", any()) }
    }

    @Test
    fun `the default key is used when nothing is stored or the stored one is blank`() = runTest {
        storage.putString("giphy_api_key", "  ")
        coEvery { remote.search("default", "gatos") } returns response(200)

        repo(defaultApiKey = "default").search("gatos")

        coVerify { remote.search("default", "gatos") }
    }

    @Test
    fun `a 200 response maps each gif to a preview and a full url`() = runTest {
        storage.putString("giphy_api_key", "k")
        coEvery { remote.search("k", "") } returns response(200, gif("s1", "o1"), gif("s2", "o2"))

        val result = repo().search("")

        assertEquals(
            GiphySearchResult.Success(listOf(GiphyGifBO("s1", "o1"), GiphyGifBO("s2", "o2"))),
            result,
        )
    }

    @Test
    fun `401 and 403 mean the api key was rejected`() = runTest {
        storage.putString("giphy_api_key", "k")
        coEvery { remote.search("k", "a") } returns response(401)
        coEvery { remote.search("k", "b") } returns response(403)

        assertEquals(GiphySearchResult.ApiKeyInvalid, repo().search("a"))
        assertEquals(GiphySearchResult.ApiKeyInvalid, repo().search("b"))
    }

    @Test
    fun `any other status is a network error`() = runTest {
        storage.putString("giphy_api_key", "k")
        coEvery { remote.search("k", "a") } returns response(500)

        assertEquals(GiphySearchResult.NetworkError, repo().search("a"))
    }

    @Test
    fun `an exception from the remote source is a network error`() = runTest {
        storage.putString("giphy_api_key", "k")
        coEvery { remote.search("k", "a") } throws java.io.IOException("offline")

        assertEquals(GiphySearchResult.NetworkError, repo().search("a"))
    }

    @Test
    fun `setApiKey stores the trimmed key and getApiKey reads it back`() {
        val repo = repo()
        assertNull(repo.getApiKey())

        repo.setApiKey("  abc123  ")

        assertEquals("abc123", repo.getApiKey())
    }
}
