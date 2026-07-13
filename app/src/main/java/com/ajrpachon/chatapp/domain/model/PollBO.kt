package com.ajrpachon.chatapp.domain.model

data class PollBO(
    val id: String,
    val conversationId: String,
    val question: String,
    val createdBy: String,
    val createdAt: Long,
)

data class PollOptionBO(
    val id: String,
    val pollId: String,
    val text: String,
    val voteCount: Int = 0,
)

data class PollVoteBO(
    val pollId: String,
    val userId: String,
    val optionId: String,
)
