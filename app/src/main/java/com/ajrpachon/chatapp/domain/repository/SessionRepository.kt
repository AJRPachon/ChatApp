package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.SessionBO
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAll(): Flow<List<SessionBO>>
    suspend fun upsert(session: SessionBO)
    suspend fun updateCurrentLastActive(ts: Long)
    suspend fun delete(sessionId: String)
    suspend fun deleteAll()
    suspend fun deleteAllOthers()
}
