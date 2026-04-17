package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.InvitationBO
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    fun observePendingInvitations(userId: String): Flow<List<InvitationBO>>
    suspend fun sendInvitation(senderId: String, receiverId: String): Result<InvitationBO>
    suspend fun acceptInvitation(invitationId: String): Result<Unit>
    suspend fun rejectInvitation(invitationId: String): Result<Unit>
}
