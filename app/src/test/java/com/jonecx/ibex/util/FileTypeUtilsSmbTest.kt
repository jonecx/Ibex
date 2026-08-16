package com.jonecx.ibex.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeUtilsSmbTest {

    @Test
    fun `smbEnsureTrailingSlash adds slash for directory`() {
        assertEquals("smb://host/share/dir/", FileTypeUtils.smbEnsureTrailingSlash("smb://host/share/dir"))
    }

    @Test
    fun `smbEnsureTrailingSlash preserves existing slash for directory`() {
        assertEquals("smb://host/share/dir/", FileTypeUtils.smbEnsureTrailingSlash("smb://host/share/dir/"))
    }

    @Test
    fun `smbEnsureTrailingSlash does not add slash when isDirectory is false`() {
        assertEquals("smb://host/share/file.txt", FileTypeUtils.smbEnsureTrailingSlash("smb://host/share/file.txt", false))
    }

    @Test
    fun `smbEnsureTrailingSlash defaults isDirectory to true`() {
        assertEquals("smb://host/share/", FileTypeUtils.smbEnsureTrailingSlash("smb://host/share"))
    }

    @Test
    fun `smbBuildChildPath builds file path without trailing slash`() {
        assertEquals(
            "smb://host/share/dir/file.txt",
            FileTypeUtils.smbBuildChildPath("smb://host/share/dir/", "file.txt", false),
        )
    }

    @Test
    fun `smbBuildChildPath builds directory path with trailing slash`() {
        assertEquals(
            "smb://host/share/dir/subdir/",
            FileTypeUtils.smbBuildChildPath("smb://host/share/dir/", "subdir", true),
        )
    }

    @Test
    fun `smbBuildChildPath trims parent trailing slash before joining`() {
        assertEquals(
            "smb://host/share/file.txt",
            FileTypeUtils.smbBuildChildPath("smb://host/share/", "file.txt", false),
        )
    }

    @Test
    fun `smbBuildChildPath handles parent without trailing slash`() {
        assertEquals(
            "smb://host/share/file.txt",
            FileTypeUtils.smbBuildChildPath("smb://host/share", "file.txt", false),
        )
    }

    @Test
    fun `smbExtractHost extracts host from standard smb path`() {
        assertEquals("192.168.1.100", FileTypeUtils.smbExtractHost("smb://192.168.1.100/share/dir/"))
    }

    @Test
    fun `smbExtractHost extracts host when port is present`() {
        assertEquals("myserver", FileTypeUtils.smbExtractHost("smb://myserver:445/share/"))
    }

    @Test
    fun `smbExtractHost extracts host from deep path`() {
        assertEquals("host", FileTypeUtils.smbExtractHost("smb://host/share/a/b/c/file.txt"))
    }

    @Test
    fun `smbExtractHost extracts host when only host is present`() {
        assertEquals("host", FileTypeUtils.smbExtractHost("smb://host"))
    }

    @Test
    fun `smbExtractHost returns null for path without scheme`() {
        assertEquals(null, FileTypeUtils.smbExtractHost("/storage/emulated/0/file.txt"))
    }

    @Test
    fun `smbExtractHost returns null for empty string`() {
        assertEquals(null, FileTypeUtils.smbExtractHost(""))
    }

    @Test
    fun `directoryChildCount returns count for detailed directory`() {
        assertEquals(3, FileTypeUtils.directoryChildCount(isDirectory = true, detailed = true) { 3 })
    }

    @Test
    fun `directoryChildCount returns null when listing yields null`() {
        assertEquals(null, FileTypeUtils.directoryChildCount(isDirectory = true, detailed = true) { null })
    }

    @Test
    fun `directoryChildCount returns null when listing throws`() {
        assertEquals(
            null,
            FileTypeUtils.directoryChildCount(isDirectory = true, detailed = true) {
                error("permission denied")
            },
        )
    }

    @Test
    fun `directoryChildCount skips counting when not detailed`() {
        var invoked = false
        val result = FileTypeUtils.directoryChildCount(isDirectory = true, detailed = false) {
            invoked = true
            9
        }
        assertEquals(null, result)
        assertEquals(false, invoked)
    }

    @Test
    fun `directoryChildCount skips counting for a file`() {
        var invoked = false
        val result = FileTypeUtils.directoryChildCount(isDirectory = false, detailed = true) {
            invoked = true
            9
        }
        assertEquals(null, result)
        assertEquals(false, invoked)
    }
}
