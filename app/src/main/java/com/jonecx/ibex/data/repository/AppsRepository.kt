package com.jonecx.ibex.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class AppsRepository(
    private val context: Context,
) : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val apps = getInstalledApps()
        emit(apps)
    }.flowOn(Dispatchers.IO)

    override fun getStorageRoots(): Flow<List<FileItem>> = flow {
        emit(emptyList())
    }

    override suspend fun getFileDetails(path: String): FileItem? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(path, 0)
            val appInfo = packageInfo.applicationInfo
            if (appInfo != null) {
                createFileItemFromApp(appInfo)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getInstalledApps(): List<FileItem> {
        val apps = mutableListOf<FileItem>()
        val packageManager = context.packageManager

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in installedApps) {
            // Only show user-installed apps (not system apps)
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                apps.add(createFileItemFromApp(appInfo))
            }
        }

        // Sort by app name
        return apps.sortedBy { it.name.lowercase() }
    }

    private fun createFileItemFromApp(appInfo: ApplicationInfo): FileItem {
        val packageManager = context.packageManager
        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val apkFile = File(appInfo.sourceDir)

        return FileItem(
            name = appName,
            path = appInfo.packageName,
            uri = Uri.parse("package:${appInfo.packageName}"),
            size = apkFile.length(),
            lastModified = apkFile.lastModified(),
            isDirectory = false,
            fileType = FileType.APK,
            mimeType = "application/vnd.android.package-archive",
        )
    }
}
