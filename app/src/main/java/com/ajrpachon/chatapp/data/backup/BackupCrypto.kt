package com.ajrpachon.chatapp.data.backup

import com.ajrpachon.chatapp.domain.model.WrongBackupPassphraseException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-based encryption for the Drive backup, so the file Google stores is opaque to everyone
 * but the person who knows the passphrase. The key is derived from the passphrase (not from a
 * device Keystore key) so the backup can be restored on a new phone.
 *
 * File layout: `MAGIC (4) | salt (16) | iv (12) | AES-256-GCM ciphertext + tag`.
 * The header is authenticated as GCM associated data, so editing it makes decryption fail.
 */
internal object BackupCrypto {
    private val MAGIC = byteArrayOf(0x43, 0x41, 0x42, 0x01) // "CAB" + format version 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 210_000
    private const val HEADER_BYTES = 4 + SALT_BYTES + IV_BYTES
    private val random = SecureRandom()

    fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = MAGIC + salt + iv
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(header)
        }
        return header + cipher.doFinal(plain)
    }

    /** Throws [WrongBackupPassphraseException] if the passphrase is wrong or the file was altered. */
    fun decrypt(data: ByteArray, passphrase: CharArray): ByteArray {
        if (data.size <= HEADER_BYTES || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw WrongBackupPassphraseException()
        }
        val salt = data.copyOfRange(MAGIC.size, MAGIC.size + SALT_BYTES)
        val iv = data.copyOfRange(MAGIC.size + SALT_BYTES, HEADER_BYTES)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
                updateAAD(data, 0, HEADER_BYTES)
            }
            cipher.doFinal(data, HEADER_BYTES, data.size - HEADER_BYTES)
        } catch (e: GeneralSecurityException) {
            throw WrongBackupPassphraseException(e)
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
