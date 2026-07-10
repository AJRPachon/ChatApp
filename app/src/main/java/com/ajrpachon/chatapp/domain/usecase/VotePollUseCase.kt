package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.utils.catchResult

class VotePollUseCase(private val pollRepository: PollRepository) {
    /**
     * Records [userId]'s vote for [optionId] in [pollId].
     */
    suspend operator fun invoke(
        pollId: String,
        userId: String,
        optionId: String,
    ): Result<Unit> = catchResult {
        require(pollId.isNotBlank()) { "pollId no puede estar vacío" }
        require(optionId.isNotBlank()) { "optionId no puede estar vacío" }
        pollRepository.vote(pollId, userId, optionId)
    }
}
