package com.ajrpachon.chatapp.data.emoji

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "emoji_prefs"
private const val KEY_RECENT = "recent_emojis"
private const val MAX_RECENT = 30

class EmojiRepository(private val context: Context) {

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val json = Json { ignoreUnknownKeys = true }

    private var _categories: List<EmojiCategory>? = null

    suspend fun getCategories(): List<EmojiCategory> = withContext(Dispatchers.IO) {
        _categories ?: loadCategories().also { _categories = it }
    }

    fun getRecent(): List<String> {
        val raw = prefs.getString(KEY_RECENT, "") ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun recordUsed(emoji: String) {
        val current = getRecent().filter { it != emoji }
        val updated = (listOf(emoji) + current).take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, updated.joinToString(",")).apply()
    }

    private fun loadCategories(): List<EmojiCategory> {
        val raw = context.assets.open("emojis.json").bufferedReader().readText()
        return json.decodeFromString(raw)
    }
}
