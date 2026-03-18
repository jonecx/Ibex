package com.jonecx.ibex.data.model

import androidx.compose.runtime.Immutable

enum class SortField {
    NAME,
    SIZE,
    DATE_MODIFIED,
    DATE_CREATED,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

@Immutable
data class SortOption(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASCENDING,
) {
    fun toComparator(): Comparator<FileItem> {
        val fieldComparator: Comparator<FileItem> = when (field) {
            SortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortField.SIZE -> compareBy { it.size }
            SortField.DATE_MODIFIED -> compareBy { it.lastModified }
            SortField.DATE_CREATED -> compareBy { it.createdAt }
        }
        val directed = if (direction == SortDirection.DESCENDING) fieldComparator.reversed() else fieldComparator
        return compareBy<FileItem> { !it.isDirectory }.then(directed)
    }

    companion object {
        val DEFAULT = SortOption()
    }
}
