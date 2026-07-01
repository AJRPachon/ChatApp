package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

@Entity(tableName = "message_read_receipts", primaryKeys = ["messageId", "userId"])
data class MessageReadReceiptDBO(
    val messageId: String,
    val userId: String,
    val readAt: Long,
)
