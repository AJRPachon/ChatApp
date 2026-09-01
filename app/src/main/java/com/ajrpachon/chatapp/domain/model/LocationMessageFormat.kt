package com.ajrpachon.chatapp.domain.model

/**
 * Single source of truth for encoding/decoding a shared-location message's plain-text wire
 * format ("📍 Mi ubicación: <mapsUrl>"). Previously the sender (ChatViewModel) and the reader
 * (ChatScreen) each hardcoded this prefix independently — a wording/emoji/host change in one
 * place would silently break the other with no compile-time signal.
 */
object LocationMessageFormat {
    private const val PREFIX = "📍 Mi ubicación: "
    private const val MAPS_URL_PREFIX = "https://maps.google.com/?q="

    /** Builds the message content sent for a shared location. */
    fun format(mapsUrl: String): String = "$PREFIX$mapsUrl"

    /** Returns the Maps URL if [content] is a location message in this format, else null. */
    fun parseMapsUrl(content: String): String? =
        if (content.startsWith(PREFIX + MAPS_URL_PREFIX)) content.removePrefix(PREFIX) else null
}
