package com.ajrpachon.chatapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionGuard(context: Context) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "session_guard",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun recordActivity() {
        prefs.edit().putLong("last_active", System.currentTimeMillis()).apply()
    }

    fun isSessionExpired(): Boolean {
        val lastActive = prefs.getLong("last_active", 0L)
        if (lastActive == 0L) return false // first launch, not expired
        return System.currentTimeMillis() - lastActive > INACTIVITY_TIMEOUT_MS
    }

    fun clearSession() {
        prefs.edit().remove("last_active").apply()
    }

    companion object {
        /** 7 days of inactivity triggers forced re-authentication. */
        private const val INACTIVITY_TIMEOUT_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
