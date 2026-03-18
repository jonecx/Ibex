package com.jonecx.ibex.fixtures

import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType

fun testFileItem(
    name: String,
    fileType: FileType = FileType.UNKNOWN,
    path: String = "/storage/$name",
    size: Long = 1024,
    isRemote: Boolean = false,
) = FileItem(
    name = name,
    path = path,
    uri = Uri.parse(if (path.startsWith("smb://")) path else "file://$path"),
    size = size,
    lastModified = System.currentTimeMillis(),
    isDirectory = fileType == FileType.DIRECTORY,
    fileType = fileType,
    isRemote = isRemote,
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

fun testRemoteFileItem(
    name: String,
    fileType: FileType = FileType.UNKNOWN,
    host: String = "192.168.1.100",
    sharePath: String = "share",
) = testFileItem(
    name = name,
    fileType = fileType,
    path = "smb://$host/$sharePath/$name",
    isRemote = true,
)

fun testRemoteDirectoryFileItem(
    name: String,
    host: String = "192.168.1.100",
    sharePath: String = "share",
) = testRemoteFileItem(name, FileType.DIRECTORY, host, sharePath)
