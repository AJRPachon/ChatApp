package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
// This class shares its simple name with the domain interface it implements — aliased to avoid
// a same-name clash (there is no "...Impl" suffix on this one, unlike its sibling repositories).
import com.ajrpachon.chatapp.domain.repository.IncognitoRepository as IncognitoRepositoryContract
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.incognitoDataStore by preferencesDataStore(name = "incognito_prefs")

class IncognitoRepository(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : IncognitoRepositoryContract {

    override fun isIncognito(conversationId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("incognito_$conversationId")
        return context.incognitoDataStore.data.map { prefs -> prefs[key] ?: false }
    }

    override suspend fun setIncognito(conversationId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("incognito_$conversationId")
        context.incognitoDataStore.edit { prefs ->
            if (enabled) prefs[key] = true else prefs.remove(key)
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(
                AnalyticsEvents.PARAM_SETTING_NAME to AnalyticsEvents.SETTING_INCOGNITO_MODE,
                AnalyticsEvents.PARAM_SETTING_VALUE to enabled,
            ),
        )
    }
}
