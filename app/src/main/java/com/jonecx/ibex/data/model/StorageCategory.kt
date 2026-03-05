package com.jonecx.ibex.data.model

import androidx.compose.ui.graphics.Color

data class StorageCategory(
    val name: String,
    val sizeBytes: Long,
    val color: Color,
)

data class StorageBreakdown(
    val totalBytes: Long,
    val usedBytes: Long,
    val categories: List<StorageCategory>,
)
