package com.jonecx.ibex.data.model

// Used vs total bytes on the primary volume, shown on the Storage tile.
data class StorageUsage(
    val usedBytes: Long,
    val totalBytes: Long,
)

// Everything the home grid needs to label its tiles.
data class HomeStats(
    val sources: Map<FileSourceType, SourceStats> = emptyMap(),
    val storageUsage: StorageUsage? = null,
)
