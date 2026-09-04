package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.EmojiCategory

interface EmojiRepository {
    suspend fun getCategories(): List<EmojiCategory>
    fun getRecent(): List<String>
    fun recordUsed(emoji: String)
}
