package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.AppLockRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock_prefs")

class AppLockRepositoryImpl(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : AppLockRepository {

    private val isEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val backgroundedAtKey = longPreferencesKey("app_lock_backgrounded_at")

    override val isEnabled: Flow<Boolean> =
        context.appLockDataStore.data.map { prefs -> prefs[isEnabledKey] ?: false }

    override val backgroundedAt: Flow<Long> =
        context.appLockDataStore.data.map { prefs -> prefs[backgroundedAtKey] ?: 0L }

    override suspend fun enable() {
        context.appLockDataStore.edit { prefs -> prefs[isEnabledKey] = true }
        analyticsTracker.logEvent(AnalyticsEvents.APP_LOCK_ENABLED)
    }

    override suspend fun disable() {
        context.appLockDataStore.edit { prefs -> prefs[isEnabledKey] = false }
    }

    override suspend fun recordBackgroundedAt(timestamp: Long) {
        context.appLockDataStore.edit { prefs -> prefs[backgroundedAtKey] = timestamp }
    }
}
