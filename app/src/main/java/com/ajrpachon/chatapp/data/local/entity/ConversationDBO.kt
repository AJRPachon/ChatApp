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
    val otherUserId: String? = null,
    val description: String? = null,
    val groupAvatarUrl: String? = null,
    val isMuted: Boolean = false,
    // Epoch millis: messages with createdAt < this are hidden for the current user. 0 = see all.
    val historyVisibleFrom: Long = 0L,
)
