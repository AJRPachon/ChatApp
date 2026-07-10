package com.ajrpachon.chatapp.data.repository

import com.ajrpachon.chatapp.data.local.dao.BroadcastListDao
import com.ajrpachon.chatapp.data.local.entity.BroadcastListDBO
import com.ajrpachon.chatapp.data.local.entity.BroadcastListMemberDBO
import com.ajrpachon.chatapp.data.mapper.toBO
import com.ajrpachon.chatapp.domain.model.BroadcastListBO
import com.ajrpachon.chatapp.domain.repository.BroadcastListRepository
import com.ajrpachon.chatapp.utils.catchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BroadcastListRepositoryImpl(private val dao: BroadcastListDao) : BroadcastListRepository {

    override fun observeAll(): Flow<List<BroadcastListBO>> =
        dao.observeAll().map { dbList ->
            dbList.map { dbo ->
                val members = catchResult {
                    dao.getMembersForList(dbo.id).map { it.toBO() }
                }.getOrDefault(emptyList())
                BroadcastListBO(
                    id = dbo.id,
                    name = dbo.name,
                    createdAt = dbo.createdAt,
                    members = members,
                )
            }
        }

    override suspend fun create(id: String, name: String, memberIds: List<String>, createdAt: Long) {
        val dbo = BroadcastListDBO(id = id, name = name, createdAt = createdAt)
        val members = memberIds.map { BroadcastListMemberDBO(listId = id, userId = it) }
        dao.insertWithMembers(dbo, members)
    }

    override suspend fun delete(listId: String) {
        dao.deleteWithMembers(listId)
    }
}
