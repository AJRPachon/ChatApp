package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.repository.GroupRepository

class PromoteGroupMemberUseCase(private val groupRepository: GroupRepository) {
    suspend fun promote(conversationId: String, userId: String): Result<Unit> =
        catchResult { groupRepository.promoteMember(conversationId, userId) }

    suspend fun demote(conversationId: String, userId: String): Result<Unit> =
        catchResult { groupRepository.demoteMember(conversationId, userId) }
}
