package com.ajrpachon.chatapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaUrlValidatorTest {

    // ── isValid — valid URLs ──────────────────────────────────────────────────

    @Test
    fun `valid Supabase storage URL returns true`() {
        val url = "https://myproject.supabase.co/storage/v1/object/public/avatars/img.jpg"
        assertTrue(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `valid Giphy media URL returns true`() {
        val url = "https://media.giphy.com/media/abc123/giphy.gif"
        assertTrue(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `valid Giphy numbered subdomain URL returns true`() {
        val url = "https://media3.giphy.com/media/abc123/giphy.gif"
        assertTrue(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `valid Giphy media4 subdomain URL returns true`() {
        val url = "https://media4.giphy.com/media/abc123/giphy.gif"
        assertTrue(MediaUrlValidator.isValid(url))
    }

    // ── isValid — invalid URLs ────────────────────────────────────────────────

    @Test
    fun `HTTP Supabase URL returns false (not https)`() {
        val url = "http://myproject.supabase.co/storage/v1/object/public/img.jpg"
        assertFalse(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `FTP URL returns false`() {
        val url = "ftp://myproject.supabase.co/img.jpg"
        assertFalse(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `JavaScript script URL returns false`() {
        val url = "javascript:alert('xss')"
        assertFalse(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `arbitrary HTTPS URL from unknown host returns false`() {
        val url = "https://evil.com/malware.exe"
        assertFalse(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `URL with supabase dot co in path but different host returns false`() {
        val url = "https://evil.com/supabase.co/img.jpg"
        assertFalse(MediaUrlValidator.isValid(url))
    }

    @Test
    fun `null URL returns false`() {
        assertFalse(MediaUrlValidator.isValid(null))
    }

    @Test
    fun `empty string URL returns false`() {
        assertFalse(MediaUrlValidator.isValid(""))
    }

    @Test
    fun `blank string URL returns false`() {
        assertFalse(MediaUrlValidator.isValid("   "))
    }

    @Test
    fun `URL without scheme returns false`() {
        val url = "myproject.supabase.co/storage/v1/object/public/img.jpg"
        assertFalse(MediaUrlValidator.isValid(url))
    }

    // ── sanitize ─────────────────────────────────────────────────────────────

    @Test
    fun `sanitize returns URL when valid`() {
        val url = "https://myproject.supabase.co/storage/v1/object/public/img.jpg"
        assertEquals(url, MediaUrlValidator.sanitize(url))
    }

    @Test
    fun `sanitize returns null for invalid URL`() {
        assertNull(MediaUrlValidator.sanitize("http://evil.com/img.jpg"))
    }

    @Test
    fun `sanitize returns null for null input`() {
        assertNull(MediaUrlValidator.sanitize(null))
    }

    @Test
    fun `sanitize returns null for empty string`() {
        assertNull(MediaUrlValidator.sanitize(""))
    }
}
