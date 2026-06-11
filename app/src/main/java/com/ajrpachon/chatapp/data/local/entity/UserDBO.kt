package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserDBO(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val createdAt: Long,  // epochMilliseconds — no java.util.*
    val isCurrentUser: Boolean = false,
)
