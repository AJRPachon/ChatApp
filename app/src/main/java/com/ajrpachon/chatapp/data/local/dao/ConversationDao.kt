package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.ConversationDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationDBO>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationDBO?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationDBO>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationDBO)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE conversations SET isMuted = :muted WHERE id = :id")
    suspend fun updateMuted(id: String, muted: Boolean)

    @Query("UPDATE conversations SET mutedUntil = :mutedUntil, isMuted = (:mutedUntil != 0) WHERE id = :id")
    suspend fun updateMutedUntil(id: String, mutedUntil: Long)

    @Query("SELECT * FROM conversations WHERE otherUserId = :userId LIMIT 1")
    suspend fun getByOtherUserId(userId: String): ConversationDBO?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ConversationDBO?>

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun resetUnreadCount(id: String)
}
