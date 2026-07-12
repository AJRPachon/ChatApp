package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.SessionDao
import com.ajrpachon.chatapp.data.local.entity.SessionDBO
import com.ajrpachon.chatapp.domain.model.SessionBO
import com.ajrpachon.chatapp.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl(private val dao: SessionDao) : SessionRepository {

    override fun observeAll(): Flow<List<SessionBO>> =
        dao.observeAll().map { list -> list.map { it.toBO() } }

    override suspend fun upsert(session: SessionBO) =
        dao.upsert(session.toDBO())

    override suspend fun updateCurrentLastActive(ts: Long) =
        dao.updateCurrentLastActive(ts)

    override suspend fun delete(sessionId: String) =
        dao.delete(sessionId)

    override suspend fun deleteAll() =
        dao.deleteAll()

    override suspend fun deleteAllOthers() =
        dao.deleteAllOthers()
}

private fun SessionDBO.toBO() = SessionBO(
    id = id,
    deviceInfo = deviceInfo,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isCurrent = isCurrent,
)

private fun SessionBO.toDBO() = SessionDBO(
    id = id,
    deviceInfo = deviceInfo,
    createdAt = createdAt,
    lastActiveAt = lastActiveAt,
    isCurrent = isCurrent,
)
