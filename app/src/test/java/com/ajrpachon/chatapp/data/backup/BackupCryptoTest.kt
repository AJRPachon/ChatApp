package com.ajrpachon.chatapp.data.backup

import com.ajrpachon.chatapp.domain.model.WrongBackupPassphraseException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    private val plain = """[{"id":"m1","content":"hola"}]""".toByteArray()
    private val passphrase = "correct horse".toCharArray()

    private fun decryptFailure(data: ByteArray, key: CharArray = passphrase): Throwable? =
        runCatching { BackupCrypto.decrypt(data, key) }.exceptionOrNull()

    @Test
    fun `a backup decrypts with the same passphrase`() {
        val encrypted = BackupCrypto.encrypt(plain, passphrase)

        assertArrayEquals(plain, BackupCrypto.decrypt(encrypted, passphrase))
    }

    @Test
    fun `the encrypted file does not contain the plaintext`() {
        val encrypted = BackupCrypto.encrypt(plain, passphrase)

        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("hola"))
    }

    @Test
    fun `two encryptions of the same data differ`() {
        assertFalse(BackupCrypto.encrypt(plain, passphrase).contentEquals(BackupCrypto.encrypt(plain, passphrase)))
    }

    @Test
    fun `a wrong passphrase is rejected`() {
        val encrypted = BackupCrypto.encrypt(plain, passphrase)

        assertTrue(decryptFailure(encrypted, "battery staple".toCharArray()) is WrongBackupPassphraseException)
    }

    @Test
    fun `a tampered ciphertext is rejected`() {
        val encrypted = BackupCrypto.encrypt(plain, passphrase)
        encrypted[encrypted.size - 1] = (encrypted.last().toInt() xor 1).toByte()

        assertTrue(decryptFailure(encrypted) is WrongBackupPassphraseException)
    }

    @Test
    fun `a tampered header is rejected`() {
        val encrypted = BackupCrypto.encrypt(plain, passphrase)
        encrypted[10] = (encrypted[10].toInt() xor 1).toByte()

        assertTrue(decryptFailure(encrypted) is WrongBackupPassphraseException)
    }

    @Test
    fun `a legacy plaintext file is not accepted as a backup`() {
        assertTrue(decryptFailure(plain) is WrongBackupPassphraseException)
    }

    @Test
    fun `an empty or truncated file is rejected`() {
        assertTrue(decryptFailure(ByteArray(0)) is WrongBackupPassphraseException)
        assertTrue(decryptFailure(BackupCrypto.encrypt(plain, passphrase).copyOf(20)) is WrongBackupPassphraseException)
    }
}
