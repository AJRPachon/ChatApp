package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "folder_conversations",
    primaryKeys = ["folderId", "conversationId"],
)
data class FolderConversationDBO(
    val folderId: String,
    val conversationId: String,
)
