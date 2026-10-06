package com.ajrpachon.chatapp.util

import com.ajrpachon.chatapp.utils.SecureStorage

/** In-memory [SecureStorage] for tests that must not touch the Android Keystore. */
class FakeSecureStorage : SecureStorage {
    val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
