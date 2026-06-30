package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatThemeDataStore by preferencesDataStore(name = "chat_theme_prefs")

class ChatThemeRepository(private val context: Context) {

    fun observe(conversationId: String): Flow<ChatTheme> {
        val key = stringPreferencesKey("chat_theme_$conversationId")
        return context.chatThemeDataStore.data.map { prefs ->
            prefs[key]?.let { name ->
                runCatching { ChatTheme.valueOf(name) }.getOrDefault(ChatTheme.DEFAULT)
            } ?: ChatTheme.DEFAULT
        }
    }

    suspend fun set(conversationId: String, theme: ChatTheme) {
        val key = stringPreferencesKey("chat_theme_$conversationId")
        context.chatThemeDataStore.edit { prefs ->
            prefs[key] = theme.name
        }
    }
}
