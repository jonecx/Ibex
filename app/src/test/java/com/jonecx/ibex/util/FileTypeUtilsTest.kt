package com.jonecx.ibex.util

import android.webkit.MimeTypeMap
import com.jonecx.ibex.data.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileTypeUtilsTest {

    @Before
    fun setup() {
        val shadow = Shadows.shadowOf(MimeTypeMap.getSingleton())
        shadow.addExtensionMimeTypeMapping("jpg", "image/jpeg")
        shadow.addExtensionMimeTypeMapping("png", "image/png")
        shadow.addExtensionMimeTypeMapping("mp4", "video/mp4")
        shadow.addExtensionMimeTypeMapping("mp3", "audio/mpeg")
        shadow.addExtensionMimeTypeMapping("pdf", "application/pdf")
        shadow.addExtensionMimeTypeMapping("txt", "text/plain")
        shadow.addExtensionMimeTypeMapping("apk", "application/vnd.android.package-archive")
        shadow.addExtensionMimeTypeMapping("zip", "application/zip")
    }

    @Test
    fun `getFileType returns DIRECTORY for directory`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "test_dir_${System.nanoTime()}")
        dir.mkdirs()
        try {
            assertEquals(FileType.DIRECTORY, FileTypeUtils.getFileType(dir))
        } finally {
            dir.delete()
        }
    }

    @Test
    fun `getFileType returns IMAGE for jpg`() = assertFileType("jpg", FileType.IMAGE)

    @Test
    fun `getFileType returns IMAGE for png`() = assertFileType("png", FileType.IMAGE)

    @Test
    fun `getFileType returns VIDEO for mp4`() = assertFileType("mp4", FileType.VIDEO)

    @Test
    fun `getFileType returns AUDIO for mp3`() = assertFileType("mp3", FileType.AUDIO)

    @Test
    fun `getFileType returns DOCUMENT for pdf`() = assertFileType("pdf", FileType.DOCUMENT)

    @Test
    fun `getFileType returns DOCUMENT for txt`() = assertFileType("txt", FileType.DOCUMENT)

    @Test
    fun `getFileType returns APK for apk`() = assertFileType("apk", FileType.APK)

    @Test
    fun `getFileType returns ARCHIVE for zip`() = assertFileType("zip", FileType.ARCHIVE)

    @Test
    fun `getFileType returns UNKNOWN for extensionless file`() = assertFileType("", FileType.UNKNOWN)

    @Test
    fun `getMimeType returns correct type for known extension`() = assertMimeType("png", "image/png")

    @Test
    fun `getMimeType returns null for unknown extension`() = assertMimeType("", null)

    private fun assertFileType(ext: String, expected: FileType) {
        withTempFile(ext) { file ->
            assertEquals(expected, FileTypeUtils.getFileType(file))
        }
    }

    private fun assertMimeType(ext: String, expected: String?) {
        withTempFile(ext) { file ->
            if (expected == null) {
                assertNull(FileTypeUtils.getMimeType(file))
            } else {
                assertEquals(expected, FileTypeUtils.getMimeType(file))
            }
        }
    }

    private fun withTempFile(ext: String, block: (File) -> Unit) {
        val suffix = if (ext.isEmpty()) "" else ".$ext"
        val file = File(System.getProperty("java.io.tmpdir"), "test_${System.nanoTime()}$suffix")
        file.createNewFile()
        try {
            block(file)
        } finally {
            file.delete()
        }
    }
}
