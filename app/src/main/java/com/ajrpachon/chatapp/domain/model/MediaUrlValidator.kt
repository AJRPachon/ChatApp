package com.ajrpachon.chatapp.domain.model

object MediaUrlValidator {
    private val ALLOWED_HOSTS = setOf(
        "supabase.co",          // Supabase Storage (*.supabase.co)
        "media.giphy.com",      // Giphy GIFs
        "media0.giphy.com",
        "media1.giphy.com",
        "media2.giphy.com",
        "media3.giphy.com",
        "media4.giphy.com",
    )

    fun isValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val host = extractHost(url) ?: return false
            val scheme = extractScheme(url) ?: return false
            scheme == "https" && ALLOWED_HOSTS.any { allowed ->
                host == allowed || host.endsWith(".$allowed")
            }
        } catch (_: Exception) {
            false
        }
    }

    fun sanitize(url: String?): String? = url?.takeIf { isValid(it) }

    // Pure Kotlin host/scheme extraction (no android.net.Uri — KMP compatible)
    private fun extractScheme(url: String): String? =
        url.substringBefore("://", "").lowercase().takeIf { it.isNotEmpty() }

    private fun extractHost(url: String): String? {
        val afterScheme = url.substringAfter("://", "").ifEmpty { return null }
        return afterScheme.substringBefore("/").substringBefore("?").substringBefore("#").lowercase()
    }
}
