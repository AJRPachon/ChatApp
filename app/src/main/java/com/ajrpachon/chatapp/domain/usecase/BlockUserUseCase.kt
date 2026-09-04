package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.AnalyticsTracker
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository
import com.ajrpachon.chatapp.utils.AnalyticsEvents

class BlockUserUseCase(
    private val invitationRepository: InvitationRepository,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker,
) {
    suspend fun block(blockedId: String): Result<Unit> {
        val currentUserId = userRepository.getCurrentUserId()
            ?: return Result.failure(Exception("No autenticado"))
        return invitationRepository.blockUser(currentUserId, blockedId)
            .onSuccess { analyticsTracker.logEvent(AnalyticsEvents.CONTACT_BLOCKED) }
    }

    suspend fun unblock(blockedId: String): Result<Unit> {
        val currentUserId = userRepository.getCurrentUserId()
            ?: return Result.failure(Exception("No autenticado"))
        return invitationRepository.unblockUser(currentUserId, blockedId)
    }
}
