package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.source.UserRemoteSource
import com.ajrpachon.chatapp.domain.model.EncryptionUnavailableException
import com.ajrpachon.chatapp.domain.model.MessageBO
import com.ajrpachon.chatapp.domain.repository.CrashReporter
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.E2EEKeyManager
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * E2EE encrypt/decrypt for [MessageRepositoryImpl], including the shared-key derivation cache.
 * Extracted out of the repository so encryption concerns (and their [CrashReporter] dependency)
 * don't inflate the repository's own constructor.
 */
class MessageE2EECoder(
    private val userRemoteSource: UserRemoteSource,
    private val crashReporter: CrashReporter,
) {
    // Shared key cache: avoids a Supabase round-trip on every encrypt/decrypt.
    // Key = (localUserId, peerUserId). Entry is populated on first use per session.
    private val sharedKeyCache = ConcurrentHashMap<Pair<String, String>, SecretKey>()
    private val keyDerivationMutex = Mutex()

    /**
     * Returns the cached shared key for [localUserId]+[peerId], deriving and caching it on
     * first call. Returns null if the peer has no public key registered yet.
     */
    private suspend fun getOrDeriveSharedKey(localUserId: String, peerId: String): SecretKey? {
        val cacheKey = localUserId to peerId
        sharedKeyCache[cacheKey]?.let { return it }
        // Mutex prevents duplicate derivations when multiple messages arrive concurrently.
        return keyDerivationMutex.withLock {
            sharedKeyCache[cacheKey]?.let { return it }
            val row = userRemoteSource.getPublicKey(peerId)
            val peerPublicKey = row?.publicKey
            if (peerPublicKey.isNullOrBlank()) return null
            E2EEKeyManager.getOrCreateKeyPair(localUserId)
            val key = E2EEKeyManager.deriveSharedKey(localUserId, peerPublicKey)
            sharedKeyCache[cacheKey] = key
            key
        }
    }

    /**
     * Encrypts [content] for [otherUserId]. Throws [EncryptionUnavailableException] when the
     * recipient has no public key or anything fails: the caller must not send the text in the clear.
     */
    suspend fun encrypt(senderId: String, otherUserId: String, content: String): String {
        val failure = try {
            val sharedKey = getOrDeriveSharedKey(senderId, otherUserId)
            if (sharedKey != null) return E2EEKeyManager.encrypt(sharedKey, content)
            AppLogger.d("E2EE", "No public key for $otherUserId — message not sent")
            null
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            AppLogger.w("E2EE", "Encryption failed, message not sent: ${e.message}")
            crashReporter.log("E2EE encrypt failed for recipient=$otherUserId — message not sent")
            crashReporter.recordException(e)
            e
        }
        throw EncryptionUnavailableException(failure)
    }

    /**
     * Decrypts [bo] using the sender's public key. Falls back to ciphertext on any error.
     * [currentUserId] is the local user; [senderId] is who sent the message.
     */
    suspend fun tryDecrypt(currentUserId: String, senderId: String, bo: MessageBO): MessageBO {
        return runCatching {
            val sharedKey = getOrDeriveSharedKey(currentUserId, senderId)
            if (sharedKey == null) {
                AppLogger.d("E2EE", "No public key for sender $senderId — cannot decrypt")
                return bo
            }
            bo.copy(content = E2EEKeyManager.decrypt(sharedKey, bo.content))
        }.getOrElse { e ->
            AppLogger.w("E2EE", "Decryption failed for msg ${bo.id}: ${e.message}")
            // Falling back to ciphertext means the user sees garbage instead of their message —
            // silent data-loss-looking bug. Worth a non-fatal report, not just a log line.
            crashReporter.log("E2EE decrypt failed: msgId=${bo.id} senderId=$senderId")
            crashReporter.recordException(e)
            bo
        }
    }
}
