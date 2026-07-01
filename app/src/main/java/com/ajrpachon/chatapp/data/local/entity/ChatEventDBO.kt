package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_events")
data class ChatEventDBO(
    @PrimaryKey val id: String,
    val conversationId: String,
    val title: String,
    val dateMs: Long,
    val location: String? = null,
    val createdBy: String,
    val createdAt: Long,
)
