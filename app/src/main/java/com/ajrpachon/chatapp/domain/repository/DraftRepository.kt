package com.ajrpachon.chatapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    suspend fun saveDraft(conversationId: String, text: String)
    fun getDraft(conversationId: String): Flow<String>
    fun getAllDrafts(): Flow<Map<String, String>>
}
