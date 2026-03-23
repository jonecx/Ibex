package com.jonecx.ibex.data.preferences

import com.jonecx.ibex.data.model.RecentFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentFoldersPreferencesContractTest {

    private fun folder(
        path: String,
        displayName: String = path.substringAfterLast('/'),
        timestamp: Long = System.currentTimeMillis(),
    ) = RecentFolder(
        path = path,
        displayName = displayName,
        timestamp = timestamp,
        sourceType = "LOCAL_STORAGE",
    )

    @Test
    fun `buildUpdatedList adds folder to empty list`() {
        val result = RecentFoldersPreferencesContract.buildUpdatedList(
            newFolder = folder("/storage/Downloads"),
            current = emptyList(),
        )
        assertEquals(1, result.size)
        assertEquals("/storage/Downloads", result.first().path)
    }

    @Test
    fun `buildUpdatedList prepends new folder`() {
        val existing = listOf(folder("/storage/DCIM"))
        val result = RecentFoldersPreferencesContract.buildUpdatedList(
            newFolder = folder("/storage/Music"),
            current = existing,
        )
        assertEquals(2, result.size)
        assertEquals("/storage/Music", result[0].path)
        assertEquals("/storage/DCIM", result[1].path)
    }

    @Test
    fun `buildUpdatedList deduplicates by path`() {
        val existing = listOf(
            folder("/storage/DCIM", timestamp = 1000),
            folder("/storage/Music", timestamp = 900),
        )
        val updated = folder("/storage/DCIM", timestamp = 2000)

        val result = RecentFoldersPreferencesContract.buildUpdatedList(updated, existing)

        assertEquals(2, result.size)
        assertEquals("/storage/DCIM", result[0].path)
        assertEquals(2000L, result[0].timestamp)
        assertEquals("/storage/Music", result[1].path)
    }

    @Test
    fun `buildUpdatedList caps at MAX_RECENT_FOLDERS`() {
        val existing = (1..RecentFoldersPreferencesContract.MAX_RECENT_FOLDERS).map {
            folder("/storage/dir$it")
        }
        val result = RecentFoldersPreferencesContract.buildUpdatedList(
            newFolder = folder("/storage/new"),
            current = existing,
        )
        assertEquals(RecentFoldersPreferencesContract.MAX_RECENT_FOLDERS, result.size)
        assertEquals("/storage/new", result.first().path)
    }

    @Test
    fun `buildUpdatedList evicts oldest when at capacity`() {
        val existing = (1..RecentFoldersPreferencesContract.MAX_RECENT_FOLDERS).map {
            folder("/storage/dir$it")
        }
        val result = RecentFoldersPreferencesContract.buildUpdatedList(
            newFolder = folder("/storage/new"),
            current = existing,
        )
        val lastOldPath = "/storage/dir${RecentFoldersPreferencesContract.MAX_RECENT_FOLDERS}"
        assertTrue(result.none { it.path == lastOldPath })
    }

    @Test
    fun `buildUpdatedList does not exceed max when revisiting existing path at capacity`() {
        val existing = (1..RecentFoldersPreferencesContract.MAX_RECENT_FOLDERS).map {
            folder("/storage/dir$it")
        }
        val result = RecentFoldersPreferencesContract.buildUpdatedList(
            newFolder = folder("/storage/dir5", timestamp = 9999),
            current = existing,
        )
        assertEquals(RecentFoldersPreferencesContract.MAX_RECENT_FOLDERS, result.size)
        assertEquals("/storage/dir5", result.first().path)
        assertEquals(9999L, result.first().timestamp)
    }

    @Test
    fun `buildUpdatedList preserves order of remaining items`() {
        val existing = listOf(
            folder("/a", timestamp = 300),
            folder("/b", timestamp = 200),
            folder("/c", timestamp = 100),
        )
        val result = RecentFoldersPreferencesContract.buildUpdatedList(
            newFolder = folder("/d", timestamp = 400),
            current = existing,
        )
        assertEquals(listOf("/d", "/a", "/b", "/c"), result.map { it.path })
    }
}
