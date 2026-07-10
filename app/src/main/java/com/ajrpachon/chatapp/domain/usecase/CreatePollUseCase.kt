package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.PollRepository
import com.ajrpachon.chatapp.utils.catchResult

class CreatePollUseCase(
    private val pollRepository: PollRepository,
    private val sendMessageUseCase: SendMessageUseCase,
) {
    /**
     * Creates a poll with [question] and [options] in [conversationId], then sends a
     * poll-reference message so participants see it in the chat.
     *
     * @return the generated poll ID wrapped in a [Result].
     */
    suspend operator fun invoke(
        conversationId: String,
        currentUserId: String,
        question: String,
        options: List<String>,
    ): Result<String> = catchResult {
        require(question.isNotBlank()) { "La pregunta no puede estar vacía" }
        require(options.size >= 2) { "Se necesitan al menos 2 opciones" }
        require(options.all { it.isNotBlank() }) { "Las opciones no pueden estar vacías" }

        val pollId = pollRepository.createPoll(
            conversationId = conversationId,
            question = question,
            createdBy = currentUserId,
            options = options,
        )
        sendMessageUseCase(conversationId, currentUserId, "poll:$pollId")
        pollId
    }
}
