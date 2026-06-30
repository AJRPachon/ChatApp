package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.model.NotificationSound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifSoundDataStore by preferencesDataStore(name = "notif_sound_prefs")

class NotificationSoundRepository(private val context: Context) {

    fun observe(conversationId: String): Flow<NotificationSound> {
        val key = stringPreferencesKey("notif_sound_$conversationId")
        return context.notifSoundDataStore.data.map { prefs ->
            prefs[key]?.let { name ->
                runCatching { NotificationSound.valueOf(name) }.getOrNull()
            } ?: NotificationSound.DEFAULT
        }
    }

    suspend fun set(conversationId: String, sound: NotificationSound) {
        val key = stringPreferencesKey("notif_sound_$conversationId")
        context.notifSoundDataStore.edit { prefs ->
            prefs[key] = sound.name
        }
    }

    suspend fun get(conversationId: String): NotificationSound {
        val key = stringPreferencesKey("notif_sound_$conversationId")
        val prefs = context.notifSoundDataStore.data.first()
        return prefs[key]?.let { name ->
            runCatching { NotificationSound.valueOf(name) }.getOrNull()
        } ?: NotificationSound.DEFAULT
    }
}
