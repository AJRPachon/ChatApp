package com.ajrpachon.chatapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageDTO(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("content") val content: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("audio_duration_ms") val audioDurationMs: Long? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("reply_to_content") val replyToContent: String? = null,
    @SerialName("reply_to_sender_name") val replyToSenderName: String? = null,
    @SerialName("call_type") val callType: String? = null,
    @SerialName("call_status") val callStatus: String? = null,
    @SerialName("call_duration") val callDuration: Int? = null,
    @SerialName("gif_url") val gifUrl: String? = null,
    @SerialName("sticker_url") val stickerUrl: String? = null,
    @SerialName("is_encrypted") val isEncrypted: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("file_mime_type") val fileMimeType: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("reply_to_status_id") val replyToStatusId: String? = null,
    @SerialName("reply_to_status_owner_id") val replyToStatusOwnerId: String? = null,
    @SerialName("reply_to_status_text") val replyToStatusText: String? = null,
    @SerialName("reply_to_status_image_url") val replyToStatusImageUrl: String? = null,
    @SerialName("reply_to_status_video_url") val replyToStatusVideoUrl: String? = null,
    @SerialName("reply_to_status_background_color") val replyToStatusBackgroundColor: Long? = null,
    @SerialName("reply_to_status_expires_at") val replyToStatusExpiresAt: String? = null,
)
