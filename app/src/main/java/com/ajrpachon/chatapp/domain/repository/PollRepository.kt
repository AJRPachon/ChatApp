package com.ajrpachon.chatapp.domain.repository

interface PollRepository {
    /**
     * Creates a poll with the given [question] and [options] inside [conversationId].
     * Returns the generated poll ID.
     */
    suspend fun createPoll(
        conversationId: String,
        question: String,
        createdBy: String,
        options: List<String>,
    ): String

    /** Records a vote by [userId] for [optionId] in [pollId]. */
    suspend fun vote(pollId: String, userId: String, optionId: String)
}
