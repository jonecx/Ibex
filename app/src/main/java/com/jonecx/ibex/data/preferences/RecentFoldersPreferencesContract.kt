package com.jonecx.ibex.data.preferences

import com.jonecx.ibex.data.model.RecentFolder
import kotlinx.coroutines.flow.Flow

interface RecentFoldersPreferencesContract {
    val recentFolders: Flow<List<RecentFolder>>
    suspend fun addRecentFolder(folder: RecentFolder)
    suspend fun clearRecentFolders()

    companion object {
        const val MAX_RECENT_FOLDERS = 10

        fun buildUpdatedList(
            newFolder: RecentFolder,
            current: List<RecentFolder>,
        ): List<RecentFolder> =
            (listOf(newFolder) + current.filter { it.path != newFolder.path })
                .take(MAX_RECENT_FOLDERS)
    }
}
