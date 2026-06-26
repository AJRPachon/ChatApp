package com.ajrpachon.chatapp.data.repository
import com.ajrpachon.chatapp.utils.catchResult

import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.source.InvitationRemoteSource
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.model.UserRelationship
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class InvitationRepositoryImpl(
    private val invitationDao: InvitationDao,
    private val userDao: UserDao,
    private val remoteSource: InvitationRemoteSource,
) : InvitationRepository {

    override fun observePendingInvitations(userId: String): Flow<List<InvitationBO>> = channelFlow {
        // Seed Room with current pending invitations from remote
        launch { catchResult { syncPendingInvitations(userId) } }

        // Realtime: new invitation arrives → resync to get full sender profile
        launch {
            catchResult {
                remoteSource.observeInvitations(userId).collect {
                    catchResult { syncPendingInvitations(userId) }
                }
            }
        }

        invitationDao.observePending(userId).map { dbos ->
            dbos.mapNotNull { dbo ->
                val sender = userDao.getById(dbo.senderId)?.toBO() ?: return@mapNotNull null
                dbo.toBO(sender)
            }
        }.collect { send(it) }
    }

    private suspend fun syncPendingInvitations(userId: String) {
        val dtos = remoteSource.getPendingInvitations(userId)
        // Upsert all senders first, then all invitations in one batch → single Room emission
        val senders = dtos.mapNotNull { it.sender }
        if (senders.isNotEmpty()) userDao.upsertAll(senders.map { it.toDBO() })
        val invitationDbos = dtos.map { it.toDBO() }
        if (invitationDbos.isNotEmpty()) invitationDao.upsertAll(invitationDbos)
    }

    override suspend fun sendInvitation(senderId: String, receiverId: String): Result<InvitationBO> =
        catchResult {
            val invitationDto = remoteSource.sendInvitation(senderId, receiverId)
            val invitationDbo = invitationDto.toDBO()
            invitationDao.upsert(invitationDbo)
            val sender = userDao.getById(senderId)?.toBO()
                ?: error("Sender user not found: $senderId")
            invitationDbo.toBO(sender)
        }

    override suspend fun acceptInvitation(invitationId: String): Result<Unit> = catchResult {
        remoteSource.updateStatus(invitationId, "accepted")
        invitationDao.updateStatus(invitationId, "accepted")
    }

    override suspend fun rejectInvitation(invitationId: String): Result<Unit> = catchResult {
        remoteSource.updateStatus(invitationId, "rejected")
        invitationDao.updateStatus(invitationId, "rejected")
    }

    override suspend fun getRelationship(currentUserId: String, otherUserId: String): UserRelationship {
        val blocked = catchResult { remoteSource.isBlocked(currentUserId, otherUserId) }.getOrDefault(false)
        if (blocked) return UserRelationship.BLOCKED

        val invitations = catchResult {
            remoteSource.getRelationshipInvitations(currentUserId, otherUserId)
        }.getOrDefault(emptyList())

        if (invitations.any { it.status == "accepted" }) return UserRelationship.CONNECTED
        val sentPending = invitations.firstOrNull { it.senderId == currentUserId && it.status == "pending" }
        if (sentPending != null) return UserRelationship.PENDING_SENT
        val receivedPending = invitations.firstOrNull { it.receiverId == currentUserId && it.status == "pending" }
        if (receivedPending != null) return UserRelationship.PENDING_RECEIVED
        return UserRelationship.NONE
    }

    override suspend fun blockUser(blockerId: String, blockedId: String): Result<Unit> =
        catchResult { remoteSource.blockUser(blockerId, blockedId) }

    override suspend fun unblockUser(blockerId: String, blockedId: String): Result<Unit> =
        catchResult { remoteSource.unblockUser(blockerId, blockedId) }

    override suspend fun getPendingReceivedInvitation(currentUserId: String, senderId: String): InvitationBO? {
        val invitations = catchResult {
            remoteSource.getRelationshipInvitations(currentUserId, senderId)
        }.getOrDefault(emptyList())
        val dto = invitations.firstOrNull { it.senderId == senderId && it.receiverId == currentUserId && it.status == "pending" }
            ?: return null
        dto.sender?.let { userDao.upsert(it.toDBO()) }
        invitationDao.upsert(dto.toDBO())
        val senderDbo = userDao.getById(dto.senderId) ?: return null
        return dto.toDBO().toBO(senderDbo.toBO())
    }
}
