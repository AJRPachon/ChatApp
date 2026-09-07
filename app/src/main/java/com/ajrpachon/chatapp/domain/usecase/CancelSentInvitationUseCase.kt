package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.InvitationRepository

class CancelSentInvitationUseCase(private val invitationRepository: InvitationRepository) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        invitationRepository.cancelSentInvitation(invitationId)
}
