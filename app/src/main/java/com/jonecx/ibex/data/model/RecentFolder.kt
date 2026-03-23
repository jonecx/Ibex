package com.jonecx.ibex.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class RecentFolder(
    val path: String,
    val displayName: String,
    val timestamp: Long,
    val sourceType: String,
    val connectionId: String? = null,
)
