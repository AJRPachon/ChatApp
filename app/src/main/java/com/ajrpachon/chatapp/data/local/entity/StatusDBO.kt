package com.ajrpachon.chatapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_status")
data class StatusDBO(
    @PrimaryKey val id: String,
    val userId: String,
    val text: String?,
    val imageUrl: String?,
    @ColumnInfo(name = "backgroundColor") val backgroundColor: Long = 0xFF1976D2,
    val createdAt: Long,
    val expiresAt: Long,
)
