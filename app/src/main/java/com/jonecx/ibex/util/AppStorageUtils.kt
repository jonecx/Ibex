package com.jonecx.ibex.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.jonecx.ibex.data.model.SourceStats
import java.io.File

// Single source of truth for the "Apps" surface: user-installed packages sized by APK bytes.
object AppStorageUtils {

    fun userInstalledApps(context: Context): List<ApplicationInfo> =
        context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }

    fun appStats(context: Context): SourceStats {
        val apps = userInstalledApps(context)
        return SourceStats(count = apps.size, sizeBytes = apps.sumOf { File(it.sourceDir).length() })
    }
}
