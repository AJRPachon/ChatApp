package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveInvitationsUseCase(private val invitationRepository: InvitationRepository) {
    operator fun invoke(userId: String): Flow<List<InvitationBO>> =
        invitationRepository.observePendingInvitations(userId)
}
