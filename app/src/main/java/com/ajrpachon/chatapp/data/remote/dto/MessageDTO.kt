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
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("reply_to_content") val replyToContent: String? = null,
    @SerialName("reply_to_sender_name") val replyToSenderName: String? = null,
    @SerialName("call_type") val callType: String? = null,
    @SerialName("call_status") val callStatus: String? = null,
    @SerialName("call_duration") val callDuration: Int? = null,
    @SerialName("gif_url") val gifUrl: String? = null,
    @SerialName("sticker_url") val stickerUrl: String? = null,
    @SerialName("is_encrypted") val isEncrypted: Boolean = false,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("edited_at") val editedAt: String? = null,
)
