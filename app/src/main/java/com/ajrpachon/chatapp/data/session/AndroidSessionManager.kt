package com.ajrpachon.chatapp.data.session

import android.content.Context
import androidx.core.content.edit
import com.ajrpachon.chatapp.utils.AppLogger
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AndroidSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        val encoded = prefs.getString(KEY_SESSION, null) ?: return@withContext null
        runCatching { json.decodeFromString<UserSession>(encoded) }
            .onFailure { AppLogger.w(TAG, "Failed to restore session, clearing", it) }
            .getOrNull()
            .also { if (it == null) deleteSession() }
    }

    override suspend fun saveSession(session: UserSession) {
        withContext(Dispatchers.IO) {
            runCatching {
                prefs.edit { putString(KEY_SESSION, json.encodeToString(UserSession.serializer(), session)) }
            }.onFailure { AppLogger.e(TAG, "Failed to save session", it) }
        }
    }

    override suspend fun deleteSession() {
        withContext(Dispatchers.IO) {
            prefs.edit { remove(KEY_SESSION) }
        }
    }

    companion object {
        private const val PREFS_NAME = "supabase_session"
        private const val KEY_SESSION = "session"
        private const val TAG = "AndroidSessionManager"
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    }
}
