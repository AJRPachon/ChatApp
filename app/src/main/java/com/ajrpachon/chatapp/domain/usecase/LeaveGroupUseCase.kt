package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.repository.GroupRepository

class LeaveGroupUseCase(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(conversationId: String, userId: String): Result<Unit> =
        catchResult { groupRepository.leaveGroup(conversationId, userId) }
}
