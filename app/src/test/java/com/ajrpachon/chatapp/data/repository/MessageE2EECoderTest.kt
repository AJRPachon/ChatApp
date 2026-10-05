package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.source.PublicKeyDTO
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MessageE2EECoderTest {

    private val userRemoteSource = mockk<UserRemoteSource>()
    private val crashReporter = mockk<CrashReporter>(relaxed = true)
    private val coder = MessageE2EECoder(userRemoteSource, crashReporter)

    private fun message(content: String) = MessageBO(
        id = "m1",
        conversationId = "c1",
        senderId = "peer",
        senderName = "Ana",
        content = content,
        isRead = false,
        isFromMe = false,
        createdAt = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `tryEncrypt sends plaintext when the recipient has no public key row`() = runTest {
        coEvery { userRemoteSource.getPublicKey("peer") } returns null

        val result = coder.tryEncrypt(senderId = "me", otherUserId = "peer", content = "hola")

        assertEquals("hola" to false, result)
    }

    @Test
    fun `tryEncrypt sends plaintext when the recipient public key is blank`() = runTest {
        coEvery { userRemoteSource.getPublicKey("peer") } returns PublicKeyDTO(id = "peer", publicKey = " ")

        val result = coder.tryEncrypt("me", "peer", "hola")

        assertEquals("hola" to false, result)
        verify(exactly = 0) { crashReporter.recordException(any()) }
    }

    @Test
    fun `tryEncrypt falls back to plaintext and reports the failure`() = runTest {
        val failure = IllegalStateException("network down")
        coEvery { userRemoteSource.getPublicKey("peer") } throws failure

        val result = coder.tryEncrypt("me", "peer", "hola")

        assertEquals("hola" to false, result)
        verify { crashReporter.recordException(failure) }
    }

    @Test
    fun `tryDecrypt returns the message untouched when the sender has no public key`() = runTest {
        coEvery { userRemoteSource.getPublicKey("peer") } returns null
        val bo = message("cipher")

        assertSame(bo, coder.tryDecrypt(currentUserId = "me", senderId = "peer", bo = bo))
        verify(exactly = 0) { crashReporter.recordException(any()) }
    }

    @Test
    fun `tryDecrypt returns the ciphertext and reports the failure`() = runTest {
        val failure = IllegalStateException("network down")
        coEvery { userRemoteSource.getPublicKey("peer") } throws failure
        val bo = message("cipher")

        assertSame(bo, coder.tryDecrypt("me", "peer", bo))
        verify { crashReporter.recordException(failure) }
    }

    @Test
    fun `a failed key lookup is not cached so the next call asks again`() = runTest {
        coEvery { userRemoteSource.getPublicKey("peer") } returns null

        coder.tryEncrypt("me", "peer", "uno")
        coder.tryEncrypt("me", "peer", "dos")

        coVerify(exactly = 2) { userRemoteSource.getPublicKey("peer") }
    }
}
