package com.ajrpachon.chatapp.utils

import android.content.Context
import com.ajrpachon.chatapp.data.session.AndroidSecureStorage

object GiphyKeyManager {

    private const val PREFS_NAME = "giphy_key_prefs"
    private const val KEY = "giphy_api_key"

    private var storage: SecureStorage? = null

    fun init(context: Context) {
        storage = AndroidSecureStorage(context.applicationContext, PREFS_NAME)
    }

    // Visible for testing — allows injecting a fake SecureStorage
    internal fun initWithStorage(store: SecureStorage) {
        storage = store
    }

    fun getKey(): String? = storage?.getString(KEY)

    fun setKey(key: String) {
        storage?.putString(KEY, key.trim())
    }

    fun clearKey() {
        storage?.remove(KEY)
    }
}
