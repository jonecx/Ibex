package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.StorageBreakdown
import com.jonecx.ibex.data.model.StorageCategory
import com.jonecx.ibex.data.repository.StorageAnalyzer
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_APPS
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_AUDIO
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_DOCUMENTS
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_IMAGES
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_OTHER
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_VIDEOS
import com.jonecx.ibex.ui.theme.SourceAppsColor
import com.jonecx.ibex.ui.theme.SourceAudioColor
import com.jonecx.ibex.ui.theme.SourceDocumentsColor
import com.jonecx.ibex.ui.theme.SourceImagesColor
import com.jonecx.ibex.ui.theme.SourceStorageColor
import com.jonecx.ibex.ui.theme.SourceVideosColor

class FakeStorageAnalyzer : StorageAnalyzer {

    var shouldFail = false

    override suspend fun analyze(): StorageBreakdown {
        if (shouldFail) throw RuntimeException("Fake analysis failure")
        return StorageBreakdown(
            totalBytes = 64_000_000_000L,
            usedBytes = 40_000_000_000L,
            categories = listOf(
                StorageCategory(CATEGORY_IMAGES, 10_000_000_000L, SourceImagesColor),
                StorageCategory(CATEGORY_VIDEOS, 12_000_000_000L, SourceVideosColor),
                StorageCategory(CATEGORY_AUDIO, 5_000_000_000L, SourceAudioColor),
                StorageCategory(CATEGORY_DOCUMENTS, 3_000_000_000L, SourceDocumentsColor),
                StorageCategory(CATEGORY_APPS, 2_000_000_000L, SourceAppsColor),
                StorageCategory(CATEGORY_OTHER, 8_000_000_000L, SourceStorageColor),
            ),
        )
    }

    fun reset() {
        shouldFail = false
    }
}
