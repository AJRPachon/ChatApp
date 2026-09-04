package com.ajrpachon.chatapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ajrpachon.chatapp.domain.model.NotificationSound
import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.NotificationSoundRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifSoundDataStore by preferencesDataStore(name = "notif_sound_prefs")

class NotificationSoundRepositoryImpl(
    private val context: Context,
    private val analyticsTracker: AnalyticsTracker,
) : NotificationSoundRepository {

    override fun observe(conversationId: String): Flow<NotificationSound> {
        val key = stringPreferencesKey("notif_sound_$conversationId")
        return context.notifSoundDataStore.data.map { prefs ->
            prefs[key]?.let { name ->
                runCatching { NotificationSound.valueOf(name) }.getOrNull()
            } ?: NotificationSound.DEFAULT
        }
    }

    override suspend fun set(conversationId: String, sound: NotificationSound) {
        val key = stringPreferencesKey("notif_sound_$conversationId")
        context.notifSoundDataStore.edit { prefs ->
            prefs[key] = sound.name
        }
        analyticsTracker.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(
                AnalyticsEvents.PARAM_SETTING_NAME to AnalyticsEvents.SETTING_NOTIFICATION_SOUND,
                AnalyticsEvents.PARAM_SETTING_VALUE to sound.name,
            ),
        )
    }

    override suspend fun get(conversationId: String): NotificationSound {
        val key = stringPreferencesKey("notif_sound_$conversationId")
        val prefs = context.notifSoundDataStore.data.first()
        return prefs[key]?.let { name ->
            runCatching { NotificationSound.valueOf(name) }.getOrNull()
        } ?: NotificationSound.DEFAULT
    }
}
