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
    fun `formatSizeWithCount appends count in parentheses`() {
        assertEquals("1.0 KB (3)", formatSizeWithCount(1024, 3))
    }

    @Test
    fun `formatSizeWithCount handles zero size and count`() {
        assertEquals("0 B (0)", formatSizeWithCount(0, 0))
    }

    @Test
    fun `formatSizeWithCountSpoken spells out items for TalkBack`() {
        assertEquals("1.0 KB, 3 items", formatSizeWithCountSpoken(1024, 3))
    }

    @Test
    fun `formatSizeWithCountSpoken uses singular for a single item`() {
        assertEquals("1.0 KB, 1 item", formatSizeWithCountSpoken(1024, 1))
    }

    @Test
    fun `formatStorageUsage formats used over total`() {
        val gib = 1024L * 1024L * 1024L
        assertEquals("221.0 GB / 256.0 GB", formatStorageUsage(221 * gib, 256 * gib))
    }

    @Test
    fun `formatStorageUsageSpoken reads used of total for TalkBack`() {
        val gib = 1024L * 1024L * 1024L
        assertEquals("221.0 GB used of 256.0 GB", formatStorageUsageSpoken(221 * gib, 256 * gib))
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
