package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.repository.GroupRepository

class AddGroupMemberUseCase(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(conversationId: String, userId: String, canSeeHistory: Boolean): Result<Unit> =
        catchResult { groupRepository.addMember(conversationId, userId, canSeeHistory) }
}
