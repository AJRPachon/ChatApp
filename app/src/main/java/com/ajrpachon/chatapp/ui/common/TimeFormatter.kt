package com.ajrpachon.chatapp.ui.common

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Formats a last-seen diff in milliseconds into a human-readable Spanish string. */
fun formatLastSeen(diffMs: Long): String = when {
    diffMs < 60_000L -> "última vez hace un momento"
    diffMs < 3_600_000L -> "última vez hace ${diffMs / 60_000} min"
    diffMs < 86_400_000L -> "última vez hace ${diffMs / 3_600_000} h"
    else -> "última vez hace ${diffMs / 86_400_000} d"
}

/** Formats disappearing-mode seconds into a compact label (e.g. "5m", "1h", "7d"). */
fun formatDisappearingDuration(seconds: Long): String = when {
    seconds <= 0L -> ""
    seconds < 3_600L -> "${seconds / 60}m"
    seconds < 86_400L -> "${seconds / 3_600}h"
    seconds < 604_800L -> "${seconds / 86_400}d"
    else -> "${seconds / 604_800}s"
}

/** Formats an [Instant] into a conversation-list time label (HH:mm, "Ayer", dd/MM, dd/MM/yy). */
@Suppress("DEPRECATION")
fun formatConversationTime(instant: Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(tz)
    val dt = instant.toLocalDateTime(tz)

    return when {
        dt.date == now.date -> {
            val h = dt.hour.toString().padStart(2, '0')
            val m = dt.minute.toString().padStart(2, '0')
            "$h:$m"
        }
        dt.date.year == now.date.year && dt.date.dayOfYear == now.date.dayOfYear - 1 -> "Ayer"
        dt.date.year == now.date.year -> {
            val day = dt.day.toString().padStart(2, '0')
            val month = dt.monthNumber.toString().padStart(2, '0')
            "$day/$month"
        }
        else -> {
            val day = dt.day.toString().padStart(2, '0')
            val month = dt.monthNumber.toString().padStart(2, '0')
            val year = dt.year.toString().takeLast(2)
            "$day/$month/$year"
        }
    }
}
