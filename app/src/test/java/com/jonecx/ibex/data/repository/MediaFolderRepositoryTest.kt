package com.jonecx.ibex.data.repository

import android.database.MatrixCursor
import android.net.Uri
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.util.MediaStoreUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaFolderRepositoryTest {

    private data class Row(val id: Long, val name: String, val data: String, val dateModified: Long)

    // Rows arrive newest-first, mirroring the DATE_MODIFIED DESC sort the repository asks MediaStore for.
    private val imageRows = listOf(
        Row(1, "newest.jpg", "$ROOT/DCIM/Camera/newest.jpg", 300),
        Row(2, "shot.jpg", "$ROOT/Pictures/Screenshots/shot.jpg", 250),
        Row(3, "older.jpg", "$ROOT/DCIM/Camera/older.jpg", 200),
        Row(4, "root.jpg", "$ROOT/root.jpg", 100),
    )

    private fun cursorOf(rows: List<Row>): MatrixCursor =
        MatrixCursor(MediaStoreUtils.PROJECTION).apply {
            rows.forEach { addRow(arrayOf<Any?>(it.id, it.name, it.data, 0L, it.dateModified, it.dateModified, "image/jpeg")) }
        }

    private fun listing(prefix: String, rows: List<Row> = imageRows, isVideo: Boolean = false): List<com.jonecx.ibex.data.model.FileItem> =
        cursorOf(rows).use { it.toMediaFolderListing(prefix, isVideo, COLLECTION, "unknown") }

    @Test
    fun `listing at root groups media by immediate subfolder with recursive count`() {
        val result = listing("$ROOT/")

        // DCIM and Pictures folders come first (insertion = recency), then the loose root file.
        assertEquals(listOf("DCIM", "Pictures", "root.jpg"), result.map { it.name })

        val dcim = result.first { it.name == "DCIM" }
        assertTrue(dcim.isDirectory)
        assertEquals(2, dcim.childCount) // both Camera images counted even though they are one level deeper
        assertEquals("$ROOT/DCIM", dcim.path)

        val loose = result.first { it.name == "root.jpg" }
        assertFalse(loose.isDirectory)
        assertEquals(FileType.IMAGE, loose.fileType)
    }

    @Test
    fun `listing uses the newest item under a folder as its cover`() {
        val dcim = listing("$ROOT/").first { it.name == "DCIM" }

        assertEquals("$ROOT/DCIM/Camera/newest.jpg", dcim.coverPath)
        assertFalse(dcim.coverIsVideo)
    }

    @Test
    fun `listing into a subfolder regroups by that folder's children`() {
        val result = listing("$ROOT/DCIM/")

        // Only DCIM's subtree remains; its two images collapse into the Camera album.
        assertEquals(listOf("Camera"), result.map { it.name })
        assertEquals(2, result.first().childCount)
    }

    @Test
    fun `listing skips rows outside the requested folder`() {
        // Pictures rows must not leak into a DCIM listing even though the cursor holds them.
        val result = listing("$ROOT/Pictures/")

        assertEquals(listOf("Screenshots"), result.map { it.name })
    }

    @Test
    fun `video listing marks folder cover as video`() {
        val rows = listOf(Row(1, "clip.mp4", "$ROOT/Movies/Trip/clip.mp4", 300))
        val movies = listing("$ROOT/", rows = rows, isVideo = true).first { it.name == "Movies" }

        assertTrue(movies.coverIsVideo)
    }

    private companion object {
        const val ROOT = "/storage/emulated/0"
        val COLLECTION: Uri = Uri.parse("content://media/external/images/media")
    }
}
