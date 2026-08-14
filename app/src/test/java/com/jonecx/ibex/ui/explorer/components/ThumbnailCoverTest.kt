package com.jonecx.ibex.ui.explorer.components

import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.fixtures.testDirectoryFileItem
import com.jonecx.ibex.fixtures.testFileItem
import com.jonecx.ibex.fixtures.testImageFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThumbnailCoverTest {

    @Test
    fun `thumbnailCover_forViewableFile_returnsItself`() {
        val photo = testImageFileItem("photo.jpg")
        assertSame(photo, photo.thumbnailCover())
    }

    @Test
    fun `thumbnailCover_forPlainFolder_returnsNull`() {
        assertNull(testDirectoryFileItem("Docs").thumbnailCover())
    }

    @Test
    fun `thumbnailCover_forNonMediaFile_returnsNull`() {
        assertNull(testFileItem("notes.txt", FileType.DOCUMENT).thumbnailCover())
    }

    @Test
    fun `thumbnailCover_forImageFolder_pointsAtCoverAsImage`() {
        val folder = testDirectoryFileItem("Camera").copy(coverPath = "/dcim/camera/cover.jpg")

        val cover = folder.thumbnailCover()

        assertEquals("/dcim/camera/cover.jpg", cover?.path)
        assertEquals(FileType.IMAGE, cover?.fileType)
    }

    @Test
    fun `thumbnailCover_forVideoFolder_pointsAtCoverAsVideo`() {
        val folder = testDirectoryFileItem("Movies").copy(coverPath = "/movies/cover.mp4", coverIsVideo = true)

        assertEquals(FileType.VIDEO, folder.thumbnailCover()?.fileType)
    }
}
