package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.InvitationDao
import com.ajrpachon.chatapp.data.local.dao.UserDao
import com.ajrpachon.chatapp.data.mapper.toDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.data.remote.source.InvitationRemoteSource
import com.ajrpachon.chatapp.domain.model.InvitationBO
import com.ajrpachon.chatapp.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InvitationRepositoryImpl(
    private val invitationDao: InvitationDao,
    private val userDao: UserDao,
    private val remoteSource: InvitationRemoteSource,
) : InvitationRepository {

    override fun observePendingInvitations(userId: String): Flow<List<InvitationBO>> =
        invitationDao.observePending(userId).map { dbos ->
            dbos.mapNotNull { dbo ->
                val sender = userDao.getById(dbo.senderId)?.toBO() ?: return@mapNotNull null
                dbo.toBO(sender)
            }
        }

    override suspend fun sendInvitation(senderId: String, receiverId: String): Result<InvitationBO> =
        runCatching {
            val dto = remoteSource.sendInvitation(senderId, receiverId)
            val dbo = dto.toDBO()
            invitationDao.upsert(dbo)
            val sender = userDao.getById(senderId)!!.toBO()
            dbo.toBO(sender)
        }

    override suspend fun acceptInvitation(invitationId: String): Result<Unit> = runCatching {
        remoteSource.updateStatus(invitationId, "accepted")
        invitationDao.updateStatus(invitationId, "accepted")
    }

    override suspend fun rejectInvitation(invitationId: String): Result<Unit> = runCatching {
        remoteSource.updateStatus(invitationId, "rejected")
        invitationDao.updateStatus(invitationId, "rejected")
    }
}
