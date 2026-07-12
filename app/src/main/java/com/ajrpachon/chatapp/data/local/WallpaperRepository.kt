package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wallpaperDataStore by preferencesDataStore(name = "chat_wallpapers")

class WallpaperRepository(private val context: Context) :
    com.ajrpachon.chatapp.domain.repository.WallpaperRepository {
    override fun getWallpaperColor(conversationId: String): Flow<Long?> =
        context.wallpaperDataStore.data.map { prefs ->
            prefs[stringPreferencesKey("color_$conversationId")]?.toLongOrNull()
        }

    override suspend fun setWallpaperColor(conversationId: String, color: Long?) {
        context.wallpaperDataStore.edit { prefs ->
            val key = stringPreferencesKey("color_$conversationId")
            if (color == null) prefs.remove(key) else prefs[key] = color.toString()
        }
    }
}
