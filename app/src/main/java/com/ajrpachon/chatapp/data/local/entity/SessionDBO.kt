package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_sessions")
data class SessionDBO(
    @PrimaryKey val id: String,
    val deviceInfo: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isCurrent: Boolean,
)
