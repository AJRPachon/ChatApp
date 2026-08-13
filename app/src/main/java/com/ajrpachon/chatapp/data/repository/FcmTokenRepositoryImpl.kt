package com.ajrpachon.chatapp.data.repository

import android.content.Context
import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.data.session.AndroidSecureStorage
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.utils.catchResult
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FcmTokenRepositoryImpl(
    private val remoteSource: FcmTokenRemoteSource,
    private val context: Context,
) : FcmTokenRepository {

    private val storage by lazy { AndroidSecureStorage(context, "fcm_prefs") }
    private val tokenMutex = Mutex()

    override fun savePendingToken(token: String) {
        storage.putString(KEY_PENDING_TOKEN, token)
    }

    override suspend fun syncToken() = tokenMutex.withLock {
        catchResult {
            val token = FirebaseMessaging.getInstance().token.await()
            AppLogger.d(TAG, "FCM token obtained: ${token.take(20)}...")
            remoteSource.upsertToken(token)
            withContext(Dispatchers.IO) { storage.remove(KEY_PENDING_TOKEN) }
            AppLogger.d(TAG, "FCM token upserted successfully")
        }.onFailure { e ->
            AppLogger.e(TAG, "syncToken failed", e)
        }
    }

    override suspend fun deleteToken() = tokenMutex.withLock {
        catchResult {
            val token = FirebaseMessaging.getInstance().token.await()
            remoteSource.deleteToken(token)
            FirebaseMessaging.getInstance().deleteToken().await()
            withContext(Dispatchers.IO) { storage.remove(KEY_PENDING_TOKEN) }
            AppLogger.d(TAG, "FCM token deleted")
        }.onFailure { e ->
            AppLogger.e(TAG, "deleteToken failed", e)
        }
    }

    override suspend fun upsertToken(token: String) {
        remoteSource.upsertToken(token)
    }

    companion object {
        private const val TAG = "FcmTokenRepositoryImpl"
        private const val KEY_PENDING_TOKEN = "pending_fcm_token"
    }
}
