package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.repository.GroupRepository

class UpdateGroupUseCase(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(
        conversationId: String,
        name: String? = null,
        description: String? = null,
        avatarUrl: String? = null,
    ): Result<Unit> = catchResult {
        groupRepository.updateGroup(conversationId, name?.trim()?.takeIf { it.isNotBlank() }, description, avatarUrl)
    }
}
