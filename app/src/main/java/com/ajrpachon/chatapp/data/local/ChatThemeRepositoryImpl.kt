package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.model.ChatTheme
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.ChatThemeRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatThemeDataStore by preferencesDataStore(name = "chat_theme_prefs")

class ChatThemeRepositoryImpl(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : ChatThemeRepository {

    override fun observe(conversationId: String): Flow<ChatTheme> {
        val key = stringPreferencesKey("chat_theme_$conversationId")
        return context.chatThemeDataStore.data.map { prefs ->
            prefs[key]?.let { name ->
                runCatching { ChatTheme.valueOf(name) }.getOrDefault(ChatTheme.DEFAULT)
            } ?: ChatTheme.DEFAULT
        }
    }

    override suspend fun set(conversationId: String, theme: ChatTheme) {
        val key = stringPreferencesKey("chat_theme_$conversationId")
        context.chatThemeDataStore.edit { prefs ->
            prefs[key] = theme.name
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(
                AnalyticsEvents.PARAM_SETTING_NAME to AnalyticsEvents.SETTING_CHAT_THEME,
                AnalyticsEvents.PARAM_SETTING_VALUE to theme.name,
            ),
        )
    }
}
