package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

@Entity(tableName = "message_reactions", primaryKeys = ["messageId", "userId", "emoji"])
data class ReactionDBO(
    val messageId: String,
    val userId: String,
    val emoji: String,
)
