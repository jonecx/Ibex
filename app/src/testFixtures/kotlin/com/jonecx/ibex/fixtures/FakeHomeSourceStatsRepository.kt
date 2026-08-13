package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.FileSource
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.FileSources
import com.jonecx.ibex.data.model.HomeStats
import com.jonecx.ibex.data.model.SourceStats
import com.jonecx.ibex.data.model.StorageUsage
import com.jonecx.ibex.data.repository.HomeSourceStatsRepository

private const val GIB = 1024L * 1024L * 1024L

// Sample home data shared by unit, instrumented, and screenshot tests.
object HomeStatsFixtures {
    val sample = HomeStats(
        sources = mapOf(
            FileSourceType.LOCAL_DOWNLOADS to SourceStats(count = 364, sizeBytes = 8_400_000_000L),
            FileSourceType.LOCAL_IMAGES to SourceStats(count = 4253, sizeBytes = 19_542_000_000L),
            FileSourceType.LOCAL_VIDEOS to SourceStats(count = 1056, sizeBytes = 68_051_000_000L),
            FileSourceType.LOCAL_AUDIO to SourceStats(count = 63, sizeBytes = 329_000_000L),
            FileSourceType.LOCAL_DOCUMENTS to SourceStats(count = 73, sizeBytes = 173_000_000L),
            FileSourceType.LOCAL_APPS to SourceStats(count = 172, sizeBytes = 54_400_000_000L),
            FileSourceType.LOCAL_RECENT to SourceStats(count = 17, sizeBytes = 180_000_000L),
            FileSourceType.LOCAL_TRASH to SourceStats(count = 5, sizeBytes = 42_000_000L),
        ),
        // Binary-round so formatFileSize yields exact "221.0 GB / 256.0 GB".
        storageUsage = StorageUsage(usedBytes = 221 * GIB, totalBytes = 256 * GIB),
    )

    // Literal root paths keep this off Environment so it renders under layoutlib in screenshot tests.
    val localSources: List<FileSource> = FileSources.getLocalSources(
        storage = "Storage",
        downloads = "Downloads",
        images = "Images",
        videos = "Videos",
        audio = "Audio",
        documents = "Documents",
        apps = "Apps",
        recent = "Recent",
        analysis = "Analysis",
        trash = "Trash",
        storageRootPath = "/storage/emulated/0",
        downloadsRootPath = "/storage/emulated/0/Download",
    )

    val remoteSources: List<FileSource> = FileSources.getRemoteSources(
        cloud = "Cloud",
        smb = "SMB/CIFS",
        ftp = "FTP",
        live = "Live",
    )
}

class FakeHomeSourceStatsRepository(
    var stats: HomeStats = HomeStatsFixtures.sample,
) : HomeSourceStatsRepository {

    var shouldFail = false

    override suspend fun loadStats(): HomeStats {
        if (shouldFail) throw RuntimeException("Fake stats failure")
        return stats
    }

    fun reset() {
        shouldFail = false
        stats = HomeStatsFixtures.sample
    }
}
