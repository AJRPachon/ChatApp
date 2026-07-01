package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM active_sessions ORDER BY lastActiveAt DESC")
    fun observeAll(): Flow<List<SessionDBO>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionDBO)

    @Query("DELETE FROM active_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)

    @Query("DELETE FROM active_sessions WHERE isCurrent = 0")
    suspend fun deleteAllOthers()

    @Query("DELETE FROM active_sessions")
    suspend fun deleteAll()

    @Query("UPDATE active_sessions SET lastActiveAt = :ts WHERE isCurrent = 1")
    suspend fun updateCurrentLastActive(ts: Long)
}
