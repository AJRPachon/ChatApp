package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invitations")
data class InvitationDBO(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderUsername: String,
    val senderDisplayName: String,
    val receiverId: String,
    val status: String,
    val createdAt: Long,
)
