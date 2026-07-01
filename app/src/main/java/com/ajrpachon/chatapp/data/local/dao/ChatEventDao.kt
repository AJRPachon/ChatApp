package com.ajrpachon.chatapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ajrpachon.chatapp.data.local.entity.ChatEventDBO
import com.ajrpachon.chatapp.data.local.entity.EventRsvpDBO
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ChatEventDBO)

    @Query("SELECT * FROM chat_events WHERE id = :eventId LIMIT 1")
    fun observeById(eventId: String): Flow<ChatEventDBO?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRsvp(rsvp: EventRsvpDBO)

    @Query("SELECT * FROM event_rsvps WHERE eventId = :eventId")
    fun getAttendees(eventId: String): Flow<List<EventRsvpDBO>>

    @Query("SELECT * FROM event_rsvps WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    fun observeMyRsvp(eventId: String, userId: String): Flow<EventRsvpDBO?>
}
