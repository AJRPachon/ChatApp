package com.ajrpachon.chatapp.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages end-to-end encryption keys for 1:1 chat conversations.
 *
 * Key exchange: ECDH over secp256r1 (P-256), keys stored in Android KeyStore.
 * Shared secret derivation: HKDF using HmacSHA256.
 * Message encryption: AES-256-GCM (IV prepended to ciphertext, Base64-encoded).
 */
object E2EEKeyManager {

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val KEY_ALIAS_PREFIX = "chatapp_e2ee_"
    private const val EC_ALGORITHM = "EC"
    private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val AES_KEY_LENGTH_BYTES = 32 // 256 bits

    /**
     * Returns the existing EC key pair for [userId] from Android KeyStore, or generates a new one.
     */
    fun getOrCreateKeyPair(userId: String): KeyPair {
        val alias = "$KEY_ALIAS_PREFIX$userId"
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

        if (keyStore.containsAlias(alias)) {
            val privateKey = keyStore.getKey(alias, null) as java.security.PrivateKey
            val certificate = keyStore.getCertificate(alias)
            return KeyPair(certificate.publicKey, privateKey)
        }

        val keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM, ANDROID_KEY_STORE)
        keyPairGenerator.initialize(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_AGREE_KEY,
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .build()
        )
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Returns the Base64-encoded DER public key for [userId].
     * Safe to share with other users (e.g., store in `profiles.public_key`).
     */
    fun getPublicKeyBase64(userId: String): String {
        val keyPair = getOrCreateKeyPair(userId)
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    /**
     * Derives a shared AES-256 secret key using ECDH + HKDF.
     *
     * @param userId               our own user ID (used to look up our private key)
     * @param otherPublicKeyBase64 Base64-encoded DER public key of the other party
     */
    fun deriveSharedKey(userId: String, otherPublicKeyBase64: String): SecretKey {
        val keyPair = getOrCreateKeyPair(userId)

        // Decode the other party's public key
        val otherKeyBytes = Base64.decode(otherPublicKeyBase64, Base64.NO_WRAP)
        val keyFactory = java.security.KeyFactory.getInstance(EC_ALGORITHM)
        val otherPublicKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(otherKeyBytes))

        // Perform ECDH key agreement
        val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM, ANDROID_KEY_STORE)
        keyAgreement.init(keyPair.private)
        keyAgreement.doPhase(otherPublicKey, true)
        val sharedSecret: ByteArray = keyAgreement.generateSecret()

        // HKDF-Extract + HKDF-Expand (single round) using HmacSHA256
        val derivedKey = hkdf(sharedSecret, info = "chatapp-e2ee-v1".toByteArray())

        return SecretKeySpec(derivedKey, "AES")
    }

    /**
     * Encrypts [plaintext] with [key] using AES-256-GCM.
     * Returns Base64(IV || ciphertext+GCM-tag).
     */
    fun encrypt(key: SecretKey, plaintext: String): String {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also {
            java.security.SecureRandom().nextBytes(it)
        }
        val cipher = javax.crypto.Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts a Base64(IV || ciphertext+GCM-tag) string produced by [encrypt].
     */
    fun decrypt(key: SecretKey, ciphertext: String): String {
        val combined = Base64.decode(ciphertext, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)

        val cipher = javax.crypto.Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Simplified HKDF using HmacSHA256 (RFC 5869).
     * Extract step uses a zero salt; expand step produces one 32-byte block.
     */
    private fun hkdf(inputKeyMaterial: ByteArray, info: ByteArray): ByteArray {
        // Extract: PRK = HMAC-SHA256(salt=zeros, IKM)
        val salt = ByteArray(32) // all-zero salt
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(inputKeyMaterial)

        // Expand: T(1) = HMAC-SHA256(PRK, info || 0x01)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(info)
        mac.update(0x01.toByte())
        val okm = mac.doFinal()

        return okm.copyOf(AES_KEY_LENGTH_BYTES)
    }
}
