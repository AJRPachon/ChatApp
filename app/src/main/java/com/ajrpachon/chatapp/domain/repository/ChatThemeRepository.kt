package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.ChatTheme
import kotlinx.coroutines.flow.Flow

interface ChatThemeRepository {
    fun observe(conversationId: String): Flow<ChatTheme>
    suspend fun set(conversationId: String, theme: ChatTheme)
}
