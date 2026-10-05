package com.ajrpachon.chatapp.domain.repository

import com.ajrpachon.chatapp.domain.model.EmojiCategoryBO

interface EmojiRepository {
    suspend fun getCategories(): List<EmojiCategoryBO>
    fun getRecent(): List<String>
    fun recordUsed(emoji: String)
}
