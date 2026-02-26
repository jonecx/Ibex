package com.jonecx.ibex.ui.viewer

import com.jonecx.ibex.data.model.FileItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaViewerArgs @Inject constructor() {
    var viewableFiles: List<FileItem> = emptyList()
    var initialIndex: Int = 0

    fun set(files: List<FileItem>, index: Int) {
        viewableFiles = files
        initialIndex = index
    }

    fun clear() {
        viewableFiles = emptyList()
        initialIndex = 0
    }
}
