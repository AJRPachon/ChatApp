package com.ajrpachon.chatapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageDBO(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: Long,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null,
    val callType: String? = null,
    val callStatus: String? = null,
    val callDuration: Int? = null,
    val gifUrl: String? = null,
    val stickerUrl: String? = null,
    @ColumnInfo(name = "isEncrypted") val isEncrypted: Boolean = false,
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
)
