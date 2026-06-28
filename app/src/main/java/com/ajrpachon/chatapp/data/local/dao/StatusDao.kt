package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.StatusDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM user_status WHERE expiresAt > :nowMs ORDER BY createdAt DESC")
    fun observeActive(nowMs: Long): Flow<List<StatusDBO>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(statuses: List<StatusDBO>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(status: StatusDBO)

    @Query("DELETE FROM user_status WHERE expiresAt <= :nowMs")
    suspend fun deleteExpired(nowMs: Long)

    @Query("SELECT * FROM user_status WHERE userId = :userId AND expiresAt > :nowMs ORDER BY createdAt DESC")
    suspend fun getByUser(userId: String, nowMs: Long): List<StatusDBO>
}
