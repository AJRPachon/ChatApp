package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.util.FakeSecureStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FcmTokenRepositoryImplTest {

    private val remote = mockk<FcmTokenRemoteSource>(relaxed = true)
    private val tokenSource = mockk<FcmTokenSource>(relaxed = true)
    private val storage = FakeSecureStorage()
    private val repo = FcmTokenRepositoryImpl(remote, tokenSource, storageProvider = { storage })

    @Test
    fun `savePendingToken keeps the token until it has been synced`() {
        repo.savePendingToken("pending-token")

        assertEquals(listOf("pending-token"), storage.values.values.toList())
    }

    @Test
    fun `syncToken uploads the current token and clears the pending one`() = runTest {
        repo.savePendingToken("old")
        coEvery { tokenSource.currentToken() } returns "fresh"

        repo.syncToken()

        coVerify { remote.upsertToken("fresh") }
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun `syncToken keeps the pending token when the upload fails`() = runTest {
        repo.savePendingToken("old")
        coEvery { tokenSource.currentToken() } returns "fresh"
        coEvery { remote.upsertToken("fresh") } throws IllegalStateException("offline")

        repo.syncToken()

        assertEquals(listOf("old"), storage.values.values.toList())
    }

    @Test
    fun `syncToken does nothing when the token cannot be read`() = runTest {
        repo.savePendingToken("old")
        coEvery { tokenSource.currentToken() } throws IllegalStateException("no play services")

        repo.syncToken()

        coVerify(exactly = 0) { remote.upsertToken(any()) }
        assertEquals(listOf("old"), storage.values.values.toList())
    }

    @Test
    fun `deleteToken removes the token on the server before invalidating it locally`() = runTest {
        repo.savePendingToken("old")
        coEvery { tokenSource.currentToken() } returns "current"

        repo.deleteToken()

        coVerifyOrder {
            remote.deleteToken("current")
            tokenSource.invalidate()
        }
        assertTrue(storage.values.isEmpty())
    }

    @Test
    fun `deleteToken does not invalidate the local token when the server call fails`() = runTest {
        repo.savePendingToken("old")
        coEvery { tokenSource.currentToken() } returns "current"
        coEvery { remote.deleteToken("current") } throws IllegalStateException("offline")

        repo.deleteToken()

        coVerify(exactly = 0) { tokenSource.invalidate() }
        assertEquals(listOf("old"), storage.values.values.toList())
    }

    @Test
    fun `upsertToken passes the given token to the server`() = runTest {
        repo.upsertToken("given")

        coVerify { remote.upsertToken("given") }
    }
}
