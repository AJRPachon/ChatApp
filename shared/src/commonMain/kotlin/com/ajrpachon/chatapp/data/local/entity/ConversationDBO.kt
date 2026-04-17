package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationDBO(
    @PrimaryKey val id: String,
    val name: String?,
    val isGroup: Boolean,
    val createdBy: String,
    val updatedAt: Long,
    val unreadCount: Int = 0,
)
