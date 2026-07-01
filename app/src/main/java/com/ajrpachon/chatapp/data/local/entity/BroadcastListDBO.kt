package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "broadcast_lists")
data class BroadcastListDBO(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long, // epochMilliseconds
)
