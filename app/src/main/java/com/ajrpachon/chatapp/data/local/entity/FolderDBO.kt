package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderDBO(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String = "#6200EE",
    val sortOrder: Int = 0,
)
