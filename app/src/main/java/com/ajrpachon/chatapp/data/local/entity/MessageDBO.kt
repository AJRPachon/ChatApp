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
    @ColumnInfo(name = "isEdited") val isEdited: Boolean = false,
    @ColumnInfo(name = "editedAt") val editedAt: Long? = null,
    // Epoch millis when the message should auto-delete locally. null = never.
    @ColumnInfo(name = "expiresAt") val expiresAt: Long? = null,
    @ColumnInfo(name = "fileUrl") val fileUrl: String? = null,
    @ColumnInfo(name = "fileName") val fileName: String? = null,
    @ColumnInfo(name = "fileSize") val fileSize: Long? = null,
    @ColumnInfo(name = "fileMimeType") val fileMimeType: String? = null,
    @ColumnInfo(name = "videoUrl") val videoUrl: String? = null,
)
