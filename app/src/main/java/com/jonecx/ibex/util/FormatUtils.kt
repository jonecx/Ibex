package com.jonecx.ibex.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = ThreadLocal.withInitial {
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups],
    )
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return dateFormat.get()!!.format(Date(timestamp))
}
