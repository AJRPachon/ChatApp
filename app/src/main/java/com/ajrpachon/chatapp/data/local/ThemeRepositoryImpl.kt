package com.ajrpachon.chatapp.data.local

import android.content.Context
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
        analyticsTracker.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(
                AnalyticsEvents.PARAM_SETTING_NAME to AnalyticsEvents.SETTING_THEME,
                AnalyticsEvents.PARAM_SETTING_VALUE to theme.name,
            ),
        )
    }
}
