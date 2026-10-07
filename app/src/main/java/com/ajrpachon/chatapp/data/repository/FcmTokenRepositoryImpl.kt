package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.domain.repository.FcmTokenRepository
import com.ajrpachon.chatapp.utils.AppLogger
import com.ajrpachon.chatapp.domain.util.catchResult
import com.ajrpachon.chatapp.utils.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FcmTokenRepositoryImpl(
    private val remoteSource: FcmTokenRemoteSource,
    private val tokenSource: FcmTokenSource,
    storageProvider: () -> SecureStorage,
) : FcmTokenRepository {

    // Lazy: creating the Keystore-backed storage is not needed until a token is saved or cleared.
    private val storage by lazy(storageProvider)
    private val tokenMutex = Mutex()

    override fun savePendingToken(token: String) {
        storage.putString(KEY_PENDING_TOKEN, token)
    }

    override suspend fun syncToken() = tokenMutex.withLock {
        catchResult {
            val token = tokenSource.currentToken()
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
            val token = tokenSource.currentToken()
            remoteSource.deleteToken(token)
            tokenSource.invalidate()
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
        const val PREFS_NAME = "fcm_prefs"
        private const val TAG = "FcmTokenRepositoryImpl"
        private const val KEY_PENDING_TOKEN = "pending_fcm_token"
    }
}
