package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents

class RespondInvitationUseCase(
    private val invitationRepository: InvitationRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend fun accept(invitationId: String): Result<Unit> =
        invitationRepository.acceptInvitation(invitationId)
            .onSuccess { analyticsTracker.logEvent(AnalyticsEvents.INVITATION_ACCEPTED) }

    suspend fun reject(invitationId: String): Result<Unit> =
        invitationRepository.rejectInvitation(invitationId)
}
