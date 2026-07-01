package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.MessageReadReceiptDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageReadReceiptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(receipt: MessageReadReceiptDBO)

    @Query("SELECT * FROM message_read_receipts WHERE messageId = :messageId")
    fun getReadersForMessage(messageId: String): Flow<List<MessageReadReceiptDBO>>

    @Query(
        """
        INSERT OR REPLACE INTO message_read_receipts (messageId, userId, readAt)
        VALUES (:messageId, :userId, :readAt)
        """
    )
    suspend fun markRead(messageId: String, userId: String, readAt: Long)
}
