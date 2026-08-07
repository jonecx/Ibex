package com.jonecx.ibex.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryScrubberTest {

    @Test
    fun `scrub replaces smb url with a hashed file_ref and drops credentials`() {
        val result = TelemetryScrubber.scrub("SMB copy failed: smb://user:pass@host/share/movie.mkv")

        val expected = "SMB copy failed: smb://${FileRef.hash("smb://host/share/movie.mkv")}"
        assertEquals(expected, result)
        assertFalse(result.contains("pass"))
        assertFalse(result.contains("movie.mkv"))
    }

    @Test
    fun `scrub drops query token from https url`() {
        val result = TelemetryScrubber.scrub("fetch https://cdn.example.com/v.mp4?token=secret done")

        assertTrue(result.startsWith("fetch https://"))
        assertTrue(result.endsWith(" done"))
        assertFalse(result.contains("secret"))
        assertFalse(result.contains("token"))
    }

    @Test
    fun `file_ref is stable and credential-independent`() {
        // Same asset via different logins hashes to one ref; query rotation does not change it.
        assertEquals(
            FileRef.hash("smb://host/share/movie.mkv"),
            FileRef.hash("smb://alice:pw@host/share/movie.mkv?sig=abc"),
        )
    }

    @Test
    fun `scrub leaves plain text untouched`() {
        val text = "playback started at 1200ms"
        assertEquals(text, TelemetryScrubber.scrub(text))
    }

    @Test
    fun `scrub only touches string property values`() {
        val result = TelemetryScrubber.scrub(
            mapOf("url" to "smb://host/secret", "count" to 5, "ok" to true),
        )
        assertEquals("smb://${FileRef.hash("smb://host/secret")}", result["url"])
        assertEquals(5, result["count"])
        assertEquals(true, result["ok"])
    }
}
