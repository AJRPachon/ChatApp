package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.incognitoDataStore by preferencesDataStore(name = "incognito_prefs")

class IncognitoRepository(private val context: Context) {

    fun isIncognito(conversationId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("incognito_$conversationId")
        return context.incognitoDataStore.data.map { prefs -> prefs[key] ?: false }
    }

    suspend fun setIncognito(conversationId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("incognito_$conversationId")
        context.incognitoDataStore.edit { prefs ->
            if (enabled) prefs[key] = true else prefs.remove(key)
        }
    }
}
