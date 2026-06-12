package com.ajrpachon.chatapp.service

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ajrpachon.chatapp.data.remote.source.FcmTokenRemoteSource
import com.ajrpachon.chatapp.utils.catchResult
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FcmTokenManager(
    private val remoteSource: FcmTokenRemoteSource,
    private val context: Context,
) {

    private val prefs by lazy { buildEncryptedPrefs(context) }
    private val tokenMutex = Mutex()

    fun savePendingToken(token: String) {
        prefs.edit().putString(KEY_PENDING_TOKEN, token).apply()
    }

    suspend fun syncToken() = tokenMutex.withLock {
        catchResult {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token obtained: ${token.take(20)}...")
            remoteSource.upsertToken(token)
            withContext(Dispatchers.IO) { prefs.edit().remove(KEY_PENDING_TOKEN).apply() }
            Log.d(TAG, "FCM token upserted successfully")
        }.onFailure { e ->
            Log.e(TAG, "syncToken failed", e)
        }
    }

    suspend fun deleteToken() = tokenMutex.withLock {
        catchResult {
            val token = FirebaseMessaging.getInstance().token.await()
            remoteSource.deleteToken(token)
            FirebaseMessaging.getInstance().deleteToken().await()
            withContext(Dispatchers.IO) { prefs.edit().remove(KEY_PENDING_TOKEN).apply() }
            Log.d(TAG, "FCM token deleted")
        }.onFailure { e ->
            Log.e(TAG, "deleteToken failed", e)
        }
    }

    companion object {
        private const val TAG = "FcmTokenManager"
        private const val KEY_PENDING_TOKEN = "pending_fcm_token"

        private fun buildEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
            context,
            "fcm_prefs",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
