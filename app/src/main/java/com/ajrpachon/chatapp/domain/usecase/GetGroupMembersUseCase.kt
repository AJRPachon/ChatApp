package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow

class GetGroupMembersUseCase(private val groupRepository: GroupRepository) {
    operator fun invoke(conversationId: String): Flow<List<GroupMemberBO>> =
        groupRepository.observeMembers(conversationId)
}
