package com.jonecx.ibex.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatFileSize returns 0 B for zero`() {
        assertEquals("0 B", formatFileSize(0))
    }

    @Test
    fun `formatFileSize returns 0 B for negative`() {
        assertEquals("0 B", formatFileSize(-1))
    }

    @Test
    fun `formatFileSize formats bytes`() {
        assertEquals("500.0 B", formatFileSize(500))
    }

    @Test
    fun `formatFileSize formats kilobytes`() {
        assertEquals("1.0 KB", formatFileSize(1024))
    }

    @Test
    fun `formatFileSize formats megabytes`() {
        assertEquals("1.0 MB", formatFileSize(1024 * 1024))
    }

    @Test
    fun `formatFileSize formats gigabytes`() {
        assertEquals("1.0 GB", formatFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun `formatFileSize formats terabytes`() {
        assertEquals("1.0 TB", formatFileSize(1024L * 1024 * 1024 * 1024))
    }

    @Test
    fun `formatFileSize formats fractional megabytes`() {
        assertEquals("1.5 MB", formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `formatDate returns empty for zero`() {
        assertEquals("", formatDate(0))
    }

    @Test
    fun `formatDate returns empty for negative`() {
        assertEquals("", formatDate(-1))
    }

    @Test
    fun `formatDate formats valid timestamp`() {
        // Jun 15, 2020 12:00:00 UTC = 1592222400000 (mid-day, mid-year to avoid timezone edge cases)
        val result = formatDate(1592222400000L)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("2020"))
    }
}
