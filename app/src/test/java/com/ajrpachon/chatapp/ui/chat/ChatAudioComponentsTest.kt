package com.ajrpachon.chatapp.ui.chat

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the amplitude serialization round-trip that lets a received voice message render its
 * sender's real recorded waveform at rest (see [serializeAmplitudes]/[parseAmplitudes] docs) —
 * plus [formatAudioDuration], previously untested.
 */
class ChatAudioComponentsTest {

    private val originalLocale: Locale = Locale.getDefault()

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `serializeAmplitudes uses a dot decimal separator regardless of device locale`() {
        // A comma-decimal default locale (e.g. Spanish/German) would otherwise make
        // "%.3f".format() emit "0,500" and corrupt this comma-delimited string on parse.
        Locale.setDefault(Locale.GERMANY)

        val serialized = serializeAmplitudes(listOf(0.5f, 0.5f))!!
        val bars = serialized.split(",")

        assertFalse("expected only '.' decimals, got: $serialized", serialized.contains("0,5"))
        bars.forEach { assertTrue("'$it' should parse as a plain dot-decimal float", it.toFloatOrNull() != null) }
    }

    @Test
    fun `serializeAmplitudes returns null for empty history`() {
        assertEquals(null, serializeAmplitudes(emptyList()))
    }

    @Test
    fun `serializeAmplitudes resamples to a fixed bar count regardless of recording length`() {
        val short = serializeAmplitudes(List(10) { 0.5f })
        val long = serializeAmplitudes(List(5000) { 0.5f })

        assertEquals(short!!.split(",").size, long!!.split(",").size)
    }

    @Test
    fun `parseAmplitudes round-trips values serialized by serializeAmplitudes`() {
        val original = listOf(0.1f, 0.9f, 0.4f, 0.6f)

        val serialized = serializeAmplitudes(original)
        val parsed = parseAmplitudes(serialized)

        assertTrue(parsed.isNotEmpty())
        parsed.forEach { assertTrue(it in 0f..1f) }
    }

    @Test
    fun `parseAmplitudes returns empty list for null input`() {
        assertEquals(emptyList<Float>(), parseAmplitudes(null))
    }

    @Test
    fun `parseAmplitudes returns empty list for blank input`() {
        assertEquals(emptyList<Float>(), parseAmplitudes("  "))
    }

    @Test
    fun `parseAmplitudes returns empty list for unparseable input instead of throwing`() {
        assertEquals(emptyList<Float>(), parseAmplitudes("not,a,waveform"))
    }

    @Test
    fun `formatAudioDuration formats minutes and seconds`() {
        assertEquals("1:05", formatAudioDuration(65_000))
        assertEquals("0:00", formatAudioDuration(0))
        assertEquals("0:00", formatAudioDuration(-500))
    }
}
