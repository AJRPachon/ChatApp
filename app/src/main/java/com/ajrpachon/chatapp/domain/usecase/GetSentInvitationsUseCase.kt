package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.repository.InvitationRepository

class GetSentInvitationsUseCase(private val invitationRepository: InvitationRepository) {
    suspend operator fun invoke(userId: String): Result<List<InvitationBO>> =
        invitationRepository.getSentInvitations(userId)
}
