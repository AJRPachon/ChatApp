package com.ajrpachon.chatapp.domain.usecase

import com.ajrpachon.chatapp.domain.model.GroupMemberBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Observes the live member list for a group conversation.
 *
 * Business rule: a blank or empty [conversationId] is invalid and returns an
 * empty flow immediately rather than propagating an erroneous query to the
 * repository, preventing unnecessary network or DB calls.
 */
class GetGroupMembersUseCase(private val groupRepository: GroupRepository) {
    operator fun invoke(conversationId: String): Flow<List<GroupMemberBO>> {
        if (conversationId.isBlank()) return emptyFlow()
        return groupRepository.observeMembers(conversationId)
    }
}
