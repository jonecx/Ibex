package com.jonecx.ibex.ui.explorer.components

import com.jonecx.ibex.fixtures.testDirectoryFileItem
import com.jonecx.ibex.fixtures.testImageFileItem
import com.jonecx.ibex.fixtures.testVideoFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DefaultFileImageRequestFactoryTest {

    private lateinit var factory: DefaultFileImageRequestFactory
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        factory = DefaultFileImageRequestFactory()
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `create returns ImageRequest with file path as data`() {
        val fileItem = testImageFileItem("photo.jpg")

        val request = factory.create(context, fileItem)

        assertEquals("/storage/photo.jpg", request.data)
    }

    @Test
    fun `create sets videoFrameMillis for video files`() {
        val fileItem = testVideoFileItem("clip.mp4")

        val request = factory.create(context, fileItem)

        assertTrue(request.parameters.count() > 0)
    }

    @Test
    fun `create does not set videoFrameMillis for image files`() {
        val fileItem = testImageFileItem("photo.jpg")

        val request = factory.create(context, fileItem)

        assertTrue(request.parameters.count() == 0)
    }

    @Test
    fun `create works for directory file type`() {
        val fileItem = testDirectoryFileItem("folder")

        val request = factory.create(context, fileItem)

        assertEquals("/storage/folder", request.data)
    }
}
