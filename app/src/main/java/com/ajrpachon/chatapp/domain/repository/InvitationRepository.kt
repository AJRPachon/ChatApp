package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    fun observePendingInvitations(userId: String): Flow<List<InvitationBO>>
    suspend fun sendInvitation(senderId: String, receiverId: String): Result<InvitationBO>
    suspend fun acceptInvitation(invitationId: String): Result<Unit>
    suspend fun rejectInvitation(invitationId: String): Result<Unit>
    suspend fun getRelationship(currentUserId: String, otherUserId: String): UserRelationship
    suspend fun getPendingReceivedInvitation(currentUserId: String, senderId: String): InvitationBO?
    suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit>
    suspend fun unblockUser(blockerId: String, blockedId: String): Result<Unit>
}
