package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
// This class shares its simple name with the domain interface it implements — aliased to avoid
// a same-name clash (there is no "...Impl" suffix on this one, unlike its sibling repositories).
import com.ajrpachon.chatapp.domain.repository.WallpaperRepository as WallpaperRepositoryContract
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wallpaperDataStore by preferencesDataStore(name = "chat_wallpapers")

class WallpaperRepository(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : WallpaperRepositoryContract {
    override fun getWallpaperColor(conversationId: String): Flow<Long?> =
        context.wallpaperDataStore.data.map { prefs ->
            prefs[stringPreferencesKey("color_$conversationId")]?.toLongOrNull()
        }

    override suspend fun setWallpaperColor(conversationId: String, color: Long?) {
        context.wallpaperDataStore.edit { prefs ->
            val key = stringPreferencesKey("color_$conversationId")
            if (color == null) prefs.remove(key) else prefs[key] = color.toString()
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(AnalyticsEvents.PARAM_SETTING_NAME to AnalyticsEvents.SETTING_CHAT_WALLPAPER),
        )
    }
}
