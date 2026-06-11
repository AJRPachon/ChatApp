package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

@Entity(tableName = "group_members", primaryKeys = ["conversationId", "userId"])
data class GroupMemberDBO(
    val conversationId: String,
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String?,
    val role: String,
    val joinedAt: Long,
)
