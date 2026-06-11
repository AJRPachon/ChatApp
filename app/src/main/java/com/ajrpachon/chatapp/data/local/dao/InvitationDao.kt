package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.InvitationDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface InvitationDao {
    @Query("SELECT * FROM invitations WHERE receiverId = :userId AND status = 'pending' ORDER BY createdAt DESC, id ASC")
    fun observePending(userId: String): Flow<List<InvitationDBO>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(invitations: List<InvitationDBO>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(invitation: InvitationDBO)

    @Query("UPDATE invitations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
