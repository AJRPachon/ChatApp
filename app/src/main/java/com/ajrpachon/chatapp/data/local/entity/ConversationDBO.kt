package com.ajrpachon.chatapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationDBO(
    @PrimaryKey val id: String,
    val name: String?,
    val isGroup: Boolean,
    val createdBy: String,
    val updatedAt: Long,
    val unreadCount: Int = 0,
    val otherUserId: String? = null,
    val description: String? = null,
    val groupAvatarUrl: String? = null,
    val isMuted: Boolean = false,
    // Epoch millis: messages with createdAt < this are hidden for the current user. 0 = see all.
    val historyVisibleFrom: Long = 0L,
    // 0 = not muted, -1 = muted forever, >0 = muted until this epoch millis timestamp
    val mutedUntil: Long = 0L,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    // 0 = off, >0 = duration in seconds for new messages to auto-expire in this conversation
    @ColumnInfo(name = "disappearing_mode_seconds") val disappearingModeSeconds: Long = 0L,
) {
    fun isEffectivelyMuted(): Boolean =
        isMuted || mutedUntil == -1L || (mutedUntil > 0L && mutedUntil > System.currentTimeMillis())
}
