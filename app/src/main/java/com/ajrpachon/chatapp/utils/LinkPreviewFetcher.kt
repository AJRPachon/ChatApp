package com.ajrpachon.chatapp.utils

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class LinkPreviewData(
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val url: String,
)

class LinkPreviewFetcher {

    private val cache = LruCache<String, LinkPreviewData>(50)

    suspend fun fetchLinkPreview(url: String): LinkPreviewData? {
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; ChatApp/1.0)")
                connection.connect()

                val finalUrl = connection.url.toString()
                val contentType = connection.contentType ?: ""

                if (!contentType.contains("text/html", ignoreCase = true)) {
                    connection.disconnect()
                    return@runCatching null
                }

                val bytes = connection.inputStream.use { stream ->
                    val buffer = ByteArray(50 * 1024)
                    val read = stream.read(buffer)
                    if (read <= 0) return@use ""
                    String(buffer, 0, read, Charsets.UTF_8)
                }
                connection.disconnect()

                val ogTitle = OG_TITLE.find(bytes)?.groupValues?.getOrNull(1)?.htmlDecode()
                val ogDesc = OG_DESC.find(bytes)?.groupValues?.getOrNull(1)?.htmlDecode()
                val ogImage = OG_IMAGE.find(bytes)?.groupValues?.getOrNull(1)?.trim()
                    ?.let { resolveUrl(it, finalUrl) }

                val title = ogTitle
                    ?: TITLE_TAG.find(bytes)?.groupValues?.getOrNull(1)?.htmlDecode()
                    ?: return@runCatching null

                if (title.isBlank()) return@runCatching null

                val preview = LinkPreviewData(
                    title = title.trim(),
                    description = ogDesc?.trim()?.takeIf { it.isNotBlank() },
                    imageUrl = ogImage?.takeIf { it.startsWith("http") },
                    url = finalUrl,
                )
                cache.put(url, preview)
                preview
            }.getOrNull()
        }
    }

    private fun resolveUrl(href: String, base: String): String {
        return when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> {
                val baseUrl = runCatching { URL(base) }.getOrNull() ?: return href
                "${baseUrl.protocol}://${baseUrl.host}$href"
            }
            else -> href
        }
    }

    private fun String.htmlDecode(): String =
        this.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")

    companion object {
        private val OG_TITLE = Regex(
            """<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val OG_DESC = Regex(
            """<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val OG_IMAGE = Regex(
            """<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val TITLE_TAG = Regex(
            """<title[^>]*>([^<]+)</title>""",
            RegexOption.IGNORE_CASE,
        )
    }
}
