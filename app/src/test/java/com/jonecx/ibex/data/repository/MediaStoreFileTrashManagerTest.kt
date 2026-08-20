package com.jonecx.ibex.data.repository

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.fixtures.testFileItem
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaStoreFileTrashManagerTest {

    private val context = RuntimeEnvironment.getApplication()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var provider: FakeMediaProvider
    private lateinit var manager: MediaStoreFileTrashManager
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        provider = Robolectric.setupContentProvider(FakeMediaProvider::class.java, MediaStore.AUTHORITY)
        manager = MediaStoreFileTrashManager(context, testDispatcher)
        tempFile = File.createTempFile("trash", ".jpg")
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun `trashFile securityException falls back to direct delete and returns true`() = runTest(testDispatcher) {
        provider.hasRow = true
        provider.throwOnUpdate = true

        val result = manager.trashFile(fileItem())

        assertTrue(result)
        assertFalse("fallback should have removed the file", tempFile.exists())
    }

    @Test
    fun `trashFile securityException with missing file returns false`() = runTest(testDispatcher) {
        provider.hasRow = true
        provider.throwOnUpdate = true
        tempFile.delete()

        val result = manager.trashFile(fileItem())

        assertFalse(result)
    }

    @Test
    fun `trashFile marks media as trashed when update succeeds`() = runTest(testDispatcher) {
        provider.hasRow = true
        provider.updateCount = 1

        val result = manager.trashFile(fileItem())

        assertTrue(result)
        assertTrue("owned media is trashed, not deleted", tempFile.exists())
    }

    private fun fileItem() = testFileItem(tempFile.name, FileType.IMAGE, path = tempFile.absolutePath)

    // Stands in for the MediaStore provider so we can force the SecurityException the real one throws on non-owned media.
    class FakeMediaProvider : ContentProvider() {
        var hasRow = false
        var throwOnUpdate = false
        var updateCount = 0

        override fun onCreate() = true

        override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor =
            MatrixCursor(arrayOf(MediaStore.Files.FileColumns._ID)).apply {
                if (hasRow) addRow(arrayOf<Any?>(1L))
            }

        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
            if (throwOnUpdate) throw SecurityException("caller does not own the target media")
            return updateCount
        }

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

        override fun getType(uri: Uri): String? = null
    }
}
