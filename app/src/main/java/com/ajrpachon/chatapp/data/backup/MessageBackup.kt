package com.ajrpachon.chatapp.data.backup

import kotlinx.serialization.Serializable

/** One message as written to the Google Drive backup file (the JSON format of `chatapp_backup.json`). */
@Serializable
internal data class MessageBackup(
    val id: String,
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
    val isEncrypted: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long? = null,
    val expiresAt: Long? = null,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val fileMimeType: String? = null,
    val videoUrl: String? = null,
    val isPinned: Boolean = false,
    val isSaved: Boolean = false,
)
