package com.ajrpachon.chatapp.domain.model

/**
 * [wireValue] is the canonical DB/wire string ("audio"/"video") — persisted in the `calls` and
 * `messages` tables, sent over FCM data payloads, and round-tripped through nav routes. Route
 * every encode/decode through [wireValue]/[fromWire] rather than re-deriving it (e.g.
 * `.name.lowercase()`), since that happens to match today only because the enum constants are
 * spelled the same as the wire strings — renaming a constant would silently break it.
 */
enum class CallType(val wireValue: String) {
    AUDIO("audio"),
    VIDEO("video"),
    ;

    companion object {
        fun fromWire(value: String?): CallType = if (value == VIDEO.wireValue) VIDEO else AUDIO
    }
}

/** [wireValue]/[fromWire]: same rationale as [CallType] — the canonical DB status string. */
enum class CallStatus(val wireValue: String) {
    RINGING("ringing"),
    ACTIVE("active"),
    ENDED("ended"),
    REJECTED("rejected"),
    MISSED("missed"),
    ;

    companion object {
        fun fromWire(value: String?): CallStatus =
            entries.firstOrNull { it.wireValue == value } ?: RINGING
    }
}
