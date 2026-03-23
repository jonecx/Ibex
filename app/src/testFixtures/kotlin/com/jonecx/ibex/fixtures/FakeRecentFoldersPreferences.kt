package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.RecentFolder
import com.jonecx.ibex.data.preferences.RecentFoldersPreferencesContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeRecentFoldersPreferences : RecentFoldersPreferencesContract {

    private val _recentFolders = MutableStateFlow<List<RecentFolder>>(emptyList())
    override val recentFolders: Flow<List<RecentFolder>> = _recentFolders

    override suspend fun addRecentFolder(folder: RecentFolder) {
        _recentFolders.update { current ->
            RecentFoldersPreferencesContract.buildUpdatedList(folder, current)
        }
    }

    override suspend fun clearRecentFolders() {
        _recentFolders.value = emptyList()
    }

    fun currentRecents(): List<RecentFolder> = _recentFolders.value
}
