package com.jonecx.ibex.ui.viewer

import com.jonecx.ibex.fixtures.testImageFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaViewerArgsTest {

    private lateinit var args: MediaViewerArgs

    @Before
    fun setup() {
        args = MediaViewerArgs()
    }

    @Test
    fun `initial state has empty list and zero index`() {
        assertTrue(args.viewableFiles.isEmpty())
        assertEquals(0, args.initialIndex)
    }

    @Test
    fun `set stores files and index`() {
        val files = listOf(
            testImageFileItem("photo1.jpg"),
            testImageFileItem("photo2.jpg"),
        )

        args.set(files, 1)

        assertEquals(files, args.viewableFiles)
        assertEquals(1, args.initialIndex)
    }

    @Test
    fun `clear resets to empty state`() {
        args.set(listOf(testImageFileItem("photo.jpg")), 5)

        args.clear()

        assertTrue(args.viewableFiles.isEmpty())
        assertEquals(0, args.initialIndex)
    }

    @Test
    fun `set overwrites previous values`() {
        val firstFiles = listOf(testImageFileItem("a.jpg"))
        val secondFiles = listOf(testImageFileItem("b.jpg"), testImageFileItem("c.jpg"))

        args.set(firstFiles, 0)
        args.set(secondFiles, 1)

        assertEquals(secondFiles, args.viewableFiles)
        assertEquals(1, args.initialIndex)
    }
}
