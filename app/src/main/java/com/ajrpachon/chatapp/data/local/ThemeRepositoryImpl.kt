package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.model.ThemePreference
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.ThemeRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

// Plain SharedPreferences mirror of the DataStore value, read synchronously by
// ChatApplication.onCreate() — see readStoredPreferenceSync() below for why.
private const val SYNC_PREFS_NAME = "theme_prefs_sync"
private const val SYNC_PREFS_KEY = "theme_preference"

class ThemeRepositoryImpl(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : ThemeRepository {

    private val themeKey = stringPreferencesKey("theme_preference")

    override fun observe(): Flow<ThemePreference> =
        context.themeDataStore.data.map { prefs ->
            when (prefs[themeKey]) {
                ThemePreference.LIGHT.name -> ThemePreference.LIGHT
                ThemePreference.DARK.name -> ThemePreference.DARK
                else -> ThemePreference.SYSTEM
            }
        }

    override suspend fun set(theme: ThemePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
        // Mirror into plain SharedPreferences too: DataStore is a suspending Flow, too slow
        // to read before the very first frame (the splash screen included), so it can't be
        // what ChatApplication.onCreate() consults to align the OS-level night mode with
        // this in-app preference before any window is created.
        context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(SYNC_PREFS_KEY, theme.name) }
        analyticsTracker.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(
                AnalyticsEvents.PARAM_SETTING_NAME to AnalyticsEvents.SETTING_THEME,
                AnalyticsEvents.PARAM_SETTING_VALUE to theme.name,
            ),
        )
    }

    companion object {
        // Synchronous read for use before Koin/DataStore are ready — see set() above.
        fun readStoredPreferenceSync(context: Context): ThemePreference {
            val raw = context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(SYNC_PREFS_KEY, null)
            return when (raw) {
                ThemePreference.LIGHT.name -> ThemePreference.LIGHT
                ThemePreference.DARK.name -> ThemePreference.DARK
                else -> ThemePreference.SYSTEM
            }
        }
    }
}
