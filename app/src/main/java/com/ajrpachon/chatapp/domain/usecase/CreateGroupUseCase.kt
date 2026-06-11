package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.model.ConversationBO
import com.ajrpachon.chatapp.domain.repository.GroupRepository

class CreateGroupUseCase(private val groupRepository: GroupRepository) {
    suspend operator fun invoke(
        name: String,
        description: String?,
        createdBy: String,
        participantIds: List<String>,
    ): Result<ConversationBO> = catchResult {
        require(name.isNotBlank()) { "El nombre del grupo no puede estar vacío" }
        require(participantIds.isNotEmpty()) { "Añade al menos un participante" }
        groupRepository.createGroup(name.trim(), description?.trim()?.takeIf { it.isNotBlank() }, createdBy, participantIds)
    }
}
