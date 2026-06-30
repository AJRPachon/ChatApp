package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "polls")
data class PollDBO(
    @PrimaryKey val id: String,
    val conversationId: String,
    val question: String,
    val createdBy: String,
    val createdAt: Long,
)
