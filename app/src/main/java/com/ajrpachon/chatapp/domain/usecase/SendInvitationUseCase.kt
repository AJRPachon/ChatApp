package com.ajrpachon.chatapp.domain.usecase
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.domain.model.UserBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.ConversationRepository
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import com.ajrpachon.chatapp.domain.repository.UserRepository

sealed interface SendInvitationResult {
    data object Sent : SendInvitationResult
    data object AlreadySent : SendInvitationResult
    data object PendingReceived : SendInvitationResult
    data object Blocked : SendInvitationResult
    data class NavigateToChat(val conversationId: String, val name: String) : SendInvitationResult
    data class Failure(val message: String) : SendInvitationResult
}

class SendInvitationUseCase(
    private val invitationRepository: InvitationRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
) {
    suspend fun checkRelationship(currentUserId: String, otherUserId: String): UserRelationship =
        invitationRepository.getRelationship(currentUserId, otherUserId)

    suspend operator fun invoke(otherUser: UserBO): SendInvitationResult {
        val currentUserId = userRepository.getCurrentUserId()
            ?: return SendInvitationResult.Failure("No autenticado")

        return when (invitationRepository.getRelationship(currentUserId, otherUser.id)) {
            UserRelationship.BLOCKED -> SendInvitationResult.Blocked
            UserRelationship.CONNECTED -> {
                val conv = catchResult {
                    conversationRepository.getOrCreateDirectConversation(currentUserId, otherUser.id)
                }.getOrElse { return SendInvitationResult.Failure(it.message ?: "Error") }
                SendInvitationResult.NavigateToChat(conv.id, otherUser.displayName)
            }
            UserRelationship.PENDING_SENT -> SendInvitationResult.AlreadySent
            UserRelationship.PENDING_RECEIVED -> {
                // Auto-accept the mutual invitation and open chat
                val inv = invitationRepository.getPendingReceivedInvitation(currentUserId, otherUser.id)
                if (inv != null) {
                    invitationRepository.acceptInvitation(inv.id)
                    val conv = catchResult {
                        conversationRepository.getOrCreateDirectConversation(currentUserId, otherUser.id)
                    }.getOrElse { return SendInvitationResult.Failure(it.message ?: "Error") }
                    SendInvitationResult.NavigateToChat(conv.id, otherUser.displayName)
                } else {
                    SendInvitationResult.PendingReceived
                }
            }
            UserRelationship.NONE -> {
                invitationRepository.sendInvitation(currentUserId, otherUser.id)
                    .fold(
                        onSuccess = { SendInvitationResult.Sent },
                        onFailure = { SendInvitationResult.Failure(it.message ?: "Error al enviar invitación") },
                    )
            }
        }
    }
}
