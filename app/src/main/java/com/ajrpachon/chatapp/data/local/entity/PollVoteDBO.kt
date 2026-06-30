package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

/** Tracks which user voted on which option to prevent double voting. */
@Entity(tableName = "poll_votes", primaryKeys = ["pollId", "userId"])
data class PollVoteDBO(
    val pollId: String,
    val userId: String,
    val optionId: String,
)
