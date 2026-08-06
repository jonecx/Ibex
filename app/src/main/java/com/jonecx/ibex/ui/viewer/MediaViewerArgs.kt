package com.jonecx.ibex.ui.viewer

import androidx.compose.runtime.staticCompositionLocalOf
import com.jonecx.ibex.data.model.FileItem

val LocalMediaViewerArgs = staticCompositionLocalOf<MediaViewerArgs> {
    error("No MediaViewerArgs provided")
}

class MediaViewerArgs() {
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
