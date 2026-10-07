package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.source.PublicKeyDTO
import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.domain.model.EncryptionUnavailableException
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageE2EECoderTest {

    private suspend fun encryptFailure(content: String = "hola"): EncryptionUnavailableException {
        val thrown = runCatching { coder.encrypt("me", "peer", content) }.exceptionOrNull()
        assertTrue("expected EncryptionUnavailableException but was $thrown", thrown is EncryptionUnavailableException)
        return thrown as EncryptionUnavailableException
    }

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
    fun `encrypt refuses to send when the recipient has no public key row`() = runTest {
        coEvery { userRemoteSource.getPublicKey("peer") } returns null

        encryptFailure()
    }

    @Test
    fun `encrypt refuses to send when the recipient public key is blank`() = runTest {
        coEvery { userRemoteSource.getPublicKey("peer") } returns PublicKeyDTO(id = "peer", publicKey = " ")

        encryptFailure()
        verify(exactly = 0) { crashReporter.recordException(any()) }
    }

    @Test
    fun `encrypt refuses to send and reports the failure when the key lookup fails`() = runTest {
        val failure = IllegalStateException("network down")
        coEvery { userRemoteSource.getPublicKey("peer") } throws failure

        val thrown = encryptFailure()

        assertSame(failure, thrown.cause)
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

        runCatching { coder.encrypt("me", "peer", "uno") }
        runCatching { coder.encrypt("me", "peer", "dos") }

        coVerify(exactly = 2) { userRemoteSource.getPublicKey("peer") }
    }
}
