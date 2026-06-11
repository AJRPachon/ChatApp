package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository

class BlockUserUseCase(
    private val invitationRepository: InvitationRepository,
    private val userRepository: UserRepository,
) {
    suspend fun block(blockedId: String): Result<Unit> {
        val currentUserId = userRepository.getCurrentUserId()
            ?: return Result.failure(Exception("No autenticado"))
        return invitationRepository.blockUser(currentUserId, blockedId)
    }

    suspend fun unblock(blockedId: String): Result<Unit> {
        val currentUserId = userRepository.getCurrentUserId()
            ?: return Result.failure(Exception("No autenticado"))
        return invitationRepository.unblockUser(currentUserId, blockedId)
    }
}
