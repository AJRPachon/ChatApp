package com.ajrpachon.chatapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stickers",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackDBO::class,
            parentColumns = ["id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("pack_id")],
)
data class StickerDBO(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "pack_id") val packId: String,
    val imageUrl: String,
    val tags: String = "",
)
