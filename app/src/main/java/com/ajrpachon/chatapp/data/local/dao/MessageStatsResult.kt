package com.ajrpachon.chatapp.data.local.dao

import androidx.room.ColumnInfo

data class ConversationMessageCount(
    @ColumnInfo(name = "conversationId") val conversationId: String,
    @ColumnInfo(name = "count") val count: Int,
)

data class DayMessageCount(
    @ColumnInfo(name = "dayEpoch") val dayEpoch: Long,
    @ColumnInfo(name = "count") val count: Int,
)
