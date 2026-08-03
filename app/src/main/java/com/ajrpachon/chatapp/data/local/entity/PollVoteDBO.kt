package com.ajrpachon.chatapp.data.local.entity

import androidx.room.Entity

/**
 * Tracks which user voted on which option.
 * optionId is part of the primary key (not just pollId+userId) so a user can hold
 * multiple simultaneous votes in a poll that allows multiple answers.
 */
@Entity(tableName = "poll_votes", primaryKeys = ["pollId", "userId", "optionId"])
data class PollVoteDBO(
    val pollId: String,
    val userId: String,
    val optionId: String,
)
