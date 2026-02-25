package com.jonecx.ibex.fixtures

import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType

fun testFileItem(
    name: String,
    fileType: FileType = FileType.UNKNOWN,
    path: String = "/storage/$name",
    size: Long = 1024,
) = FileItem(
    name = name,
    path = path,
    uri = Uri.parse("file://$path"),
    size = size,
    lastModified = System.currentTimeMillis(),
    isDirectory = fileType == FileType.DIRECTORY,
    fileType = fileType,
)

fun testImageFileItem(
    name: String,
    path: String = "/storage/$name",
) = testFileItem(name, FileType.IMAGE, path)

fun testVideoFileItem(
    name: String,
    path: String = "/storage/$name",
) = testFileItem(name, FileType.VIDEO, path)

fun testDirectoryFileItem(
    name: String,
    path: String = "/storage/$name",
) = testFileItem(name, FileType.DIRECTORY, path)
