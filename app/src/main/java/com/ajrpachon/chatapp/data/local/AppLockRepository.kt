package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock_prefs")

class AppLockRepository(private val context: Context) {

    private val IS_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
    private val BACKGROUNDED_AT_KEY = longPreferencesKey("app_lock_backgrounded_at")

    val isEnabled: Flow<Boolean> =
        context.appLockDataStore.data.map { prefs -> prefs[IS_ENABLED_KEY] ?: false }

    val backgroundedAt: Flow<Long> =
        context.appLockDataStore.data.map { prefs -> prefs[BACKGROUNDED_AT_KEY] ?: 0L }

    suspend fun enable() {
        context.appLockDataStore.edit { prefs -> prefs[IS_ENABLED_KEY] = true }
    }

    suspend fun disable() {
        context.appLockDataStore.edit { prefs -> prefs[IS_ENABLED_KEY] = false }
    }

    suspend fun recordBackgroundedAt(timestamp: Long) {
        context.appLockDataStore.edit { prefs -> prefs[BACKGROUNDED_AT_KEY] = timestamp }
    }
}
