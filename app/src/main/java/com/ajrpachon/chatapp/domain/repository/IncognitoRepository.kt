package com.ajrpachon.chatapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface IncognitoRepository {
    fun isIncognito(conversationId: String): Flow<Boolean>
    suspend fun setIncognito(conversationId: String, enabled: Boolean)
}
