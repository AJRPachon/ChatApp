package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.NotificationSound
import kotlinx.coroutines.flow.Flow

interface NotificationSoundRepository {
    fun observe(conversationId: String): Flow<NotificationSound>
    suspend fun set(conversationId: String, sound: NotificationSound)
    suspend fun get(conversationId: String): NotificationSound
}
