package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.ScheduledMessageDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ScheduledMessageDBO)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledAtMs ASC")
    fun observeAll(): Flow<List<ScheduledMessageDBO>>

    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledAtMs ASC")
    suspend fun getAll(): List<ScheduledMessageDBO>

    @Query("SELECT * FROM scheduled_messages WHERE scheduledAtMs <= :nowMs ORDER BY scheduledAtMs ASC")
    suspend fun getPending(nowMs: Long): List<ScheduledMessageDBO>
}
