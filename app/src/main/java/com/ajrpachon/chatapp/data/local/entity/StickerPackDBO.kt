package com.ajrpachon.chatapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sticker_packs")
data class StickerPackDBO(
    @PrimaryKey val id: String,
    val name: String,
    val coverUrl: String,
    @ColumnInfo(name = "is_installed") val isInstalled: Boolean = false,
)
