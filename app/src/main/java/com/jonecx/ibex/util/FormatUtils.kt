package com.jonecx.ibex.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

private val dateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        bytes / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups],
    )
}

// Home tile subtitle, e.g. "63.4 GB (1056)".
fun formatSizeWithCount(bytes: Long, count: Int): String = "${formatFileSize(bytes)} ($count)"

// Storage tile subtitle, e.g. "221 GB / 256 GB".
fun formatStorageUsage(usedBytes: Long, totalBytes: Long): String =
    "${formatFileSize(usedBytes)} / ${formatFileSize(totalBytes)}"

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return dateFormat.get()!!.format(Date(timestamp))
}
