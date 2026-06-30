package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "poll_options")
data class PollOptionDBO(
    @PrimaryKey val id: String,
    val pollId: String,
    val text: String,
    val voteCount: Int = 0,
)
