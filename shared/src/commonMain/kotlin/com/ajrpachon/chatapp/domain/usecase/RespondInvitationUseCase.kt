package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.InvitationRepository

class RespondInvitationUseCase(private val invitationRepository: InvitationRepository) {
    suspend fun accept(invitationId: String): Result<Unit> =
        invitationRepository.acceptInvitation(invitationId)

    suspend fun reject(invitationId: String): Result<Unit> =
        invitationRepository.rejectInvitation(invitationId)
}
