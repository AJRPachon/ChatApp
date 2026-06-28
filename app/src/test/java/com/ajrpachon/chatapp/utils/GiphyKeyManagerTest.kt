package com.ajrpachon.chatapp.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GiphyKeyManagerTest {

    // In-memory SecureStorage — avoids AndroidKeyStore which isn't available in unit tests
    private val fakeStorage = object : SecureStorage {
        private val map = mutableMapOf<String, String>()
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
    }

    @Before
    fun setUp() {
        GiphyKeyManager.initWithStorage(fakeStorage)
        GiphyKeyManager.clearKey()
    }

    @Test
    fun `getKey returns null when no key has been set`() {
        assertNull(GiphyKeyManager.getKey())
    }

    @Test
    fun `setKey and getKey round-trip`() {
        GiphyKeyManager.setKey("my-api-key-123")
        assertEquals("my-api-key-123", GiphyKeyManager.getKey())
    }

    @Test
    fun `setKey trims whitespace`() {
        GiphyKeyManager.setKey("  abc123  ")
        assertEquals("abc123", GiphyKeyManager.getKey())
    }

    @Test
    fun `setKey overwrites previous key`() {
        GiphyKeyManager.setKey("key-v1")
        GiphyKeyManager.setKey("key-v2")
        assertEquals("key-v2", GiphyKeyManager.getKey())
    }

    @Test
    fun `clearKey removes stored key`() {
        GiphyKeyManager.setKey("some-key")
        GiphyKeyManager.clearKey()
        assertNull(GiphyKeyManager.getKey())
    }
}
