package com.ajrpachon.chatapp.data.session

import android.content.Context
import com.ajrpachon.chatapp.utils.AppLogger
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AndroidSessionManager(context: Context) : SessionManager {

    private val storage = AndroidSecureStorage(context, PREFS_NAME)

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val encoded = storage.getString(KEY_SESSION) ?: return@withContext null
        runCatching { json.decodeFromString<UserSession>(encoded) }
            .onFailure { AppLogger.w(TAG, "Failed to restore session, clearing", it) }
            .getOrNull()
            .also { if (it == null) deleteSession() }
    }

    override suspend fun saveSession(session: UserSession) {
        withContext(Dispatchers.IO) {
            runCatching {
                storage.putString(KEY_SESSION, json.encodeToString(UserSession.serializer(), session))
            }.onFailure { AppLogger.e(TAG, "Failed to save session", it) }
        }
    }

    override suspend fun deleteSession() {
        withContext(Dispatchers.IO) {
            storage.remove(KEY_SESSION)
        }
    }

    companion object {
        private const val PREFS_NAME = "supabase_session"
        private const val KEY_SESSION = "session"
        private const val TAG = "AndroidSessionManager"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
