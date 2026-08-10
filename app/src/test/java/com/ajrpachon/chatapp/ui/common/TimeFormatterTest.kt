package com.ajrpachon.chatapp.ui.common

import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    private fun createdAtMsAgo(ms: Long) =
        Instant.fromEpochMilliseconds(System.currentTimeMillis() - ms)

    @Test
    fun `just posted shows 1m instead of 0m`() {
        assertEquals("1m", formatStatusAge(createdAtMsAgo(0)))
    }

    @Test
    fun `under an hour shows minutes`() {
        assertEquals("45m", formatStatusAge(createdAtMsAgo(45 * 60_000L)))
    }

    @Test
    fun `59 minutes still shows minutes, not an hour`() {
        assertEquals("59m", formatStatusAge(createdAtMsAgo(59 * 60_000L)))
    }

    @Test
    fun `exactly one hour switches to hours`() {
        assertEquals("1h", formatStatusAge(createdAtMsAgo(60 * 60_000L)))
    }

    @Test
    fun `several hours shows whole hours only, no minutes`() {
        assertEquals("13h", formatStatusAge(createdAtMsAgo(13 * 3_600_000L + 40 * 60_000L)))
    }

    @Test
    fun `just under 24h shows 23h`() {
        assertEquals("23h", formatStatusAge(createdAtMsAgo(23 * 3_600_000L + 59 * 60_000L)))
    }
}
