package com.ajrpachon.chatapp.utils

import android.util.LruCache
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device translation using ML Kit.
 * Caches up to 100 translations to avoid redundant model calls.
 */
class TranslationManager {

    private val cache = LruCache<String, String>(100)

    /**
     * Translates [text] from Spanish to [targetLang] (a [TranslateLanguage] constant).
     * Downloads the translation model on first use if not already present.
     */
    suspend fun translate(text: String, targetLang: String = TranslateLanguage.ENGLISH): String {
        val cacheKey = "$targetLang|$text"
        cache.get(cacheKey)?.let { return it }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.SPANISH)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)
        return try {
            translator.downloadModelIfNeeded().await()
            val result = translator.translate(text).await()
            cache.put(cacheKey, result)
            result
        } finally {
            translator.close()
        }
    }
}
