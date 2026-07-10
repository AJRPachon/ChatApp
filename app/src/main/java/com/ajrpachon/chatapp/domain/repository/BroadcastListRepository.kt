package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.BroadcastListBO
import kotlinx.coroutines.flow.Flow

interface BroadcastListRepository {
    fun observeAll(): Flow<List<BroadcastListBO>>
    suspend fun create(id: String, name: String, memberIds: List<String>, createdAt: Long)
    suspend fun delete(listId: String)
}
