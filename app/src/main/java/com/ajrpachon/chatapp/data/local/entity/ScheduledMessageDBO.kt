package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageDBO(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val scheduledAtMs: Long,
    val createdAt: Long,
)
