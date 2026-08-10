package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemeRepositoryImpl(private val context: Context) :
    com.ajrpachon.chatapp.domain.repository.ThemeRepository {

    private val THEME_KEY = stringPreferencesKey("theme_preference")

    override fun observe(): Flow<ThemePreference> =
        context.themeDataStore.data.map { prefs ->
            when (prefs[THEME_KEY]) {
                ThemePreference.LIGHT.name -> ThemePreference.LIGHT
                ThemePreference.DARK.name -> ThemePreference.DARK
                else -> ThemePreference.SYSTEM
            }
        }

    override suspend fun set(theme: ThemePreference) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }
}
