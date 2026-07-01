package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "broadcast_list_members",
    primaryKeys = ["listId", "userId"],
)
data class BroadcastListMemberDBO(
    val listId: String,
    val userId: String,
)
