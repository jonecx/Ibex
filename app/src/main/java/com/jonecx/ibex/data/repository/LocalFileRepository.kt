package com.jonecx.ibex.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.util.FileTypeUtils.toFileItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class LocalFileRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : FileRepository {

    private var cachedTrashedPaths: Set<String> = emptySet()
    private var trashedPathsCacheTime: Long = 0L

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val directory = File(path)
        if (directory.exists() && directory.isDirectory) {
            val trashedPaths = getTrashedFilePaths()
            val files = directory.listFiles()
                ?.filter { file -> file.absolutePath !in trashedPaths }
                ?.map { file -> file.toFileItem() }
                ?: emptyList()
            emit(files)
        } else {
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)

    private fun getTrashedFilePaths(): Set<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return emptySet()
        }

        val now = System.currentTimeMillis()
        if (now - trashedPathsCacheTime < TRASH_CACHE_TTL_MS) {
            return cachedTrashedPaths
        }

        val trashedPaths = mutableSetOf<String>()
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.IS_TRASHED} = ?"
        val selectionArgs = arrayOf("1")

        val queryArgs = android.os.Bundle().apply {
            putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                if (path != null) {
                    trashedPaths.add(path)
                }
            }
        }

        cachedTrashedPaths = trashedPaths
        trashedPathsCacheTime = System.currentTimeMillis()
        return trashedPaths
    }

    companion object {
        private const val TRASH_CACHE_TTL_MS = 5_000L
    }

    override fun getStorageRoots(): Flow<List<FileItem>> = flow {
        val roots = mutableListOf<FileItem>()

        val internalStorage = Environment.getExternalStorageDirectory()
        if (internalStorage.exists()) {
            roots.add(internalStorage.toStorageRoot(context.getString(R.string.internal_storage)))
        }

        context.getExternalFilesDirs(null).forEachIndexed { index, file ->
            if (index > 0 && file != null) {
                val sdCardRoot = findSdCardRoot(file)
                if (sdCardRoot != null && sdCardRoot.exists()) {
                    roots.add(sdCardRoot.toStorageRoot(context.getString(R.string.sd_card)))
                }
            }
        }

        emit(roots)
    }.flowOn(ioDispatcher)

    private fun File.toStorageRoot(displayName: String): FileItem = FileItem(
        name = displayName,
        path = absolutePath,
        uri = toUri(),
        size = totalSpace,
        lastModified = lastModified(),
        isDirectory = true,
        fileType = FileType.DIRECTORY,
        childCount = listFiles()?.size,
    )

    private fun findSdCardRoot(file: File): File? {
        var current = file
        while (current.parentFile != null) {
            if (current.parentFile?.name == "storage") {
                return current
            }
            current = current.parentFile!!
        }
        return null
    }
}
