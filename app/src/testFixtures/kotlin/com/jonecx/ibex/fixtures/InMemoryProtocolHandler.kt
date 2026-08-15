package com.jonecx.ibex.fixtures

import android.net.Uri
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.repository.ProtocolFileHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val MEM_SCHEME = "mem:/"

fun memFileItem(path: String, size: Long, isDirectory: Boolean = false): FileItem = FileItem(
    name = path.trimEnd('/').substringAfterLast('/'),
    path = path,
    uri = Uri.EMPTY,
    size = size,
    lastModified = 0L,
    isDirectory = isDirectory,
    fileType = if (isDirectory) FileType.DIRECTORY else FileType.UNKNOWN,
)

// In-memory ProtocolFileHandler for transfer tests: a flat path -> bytes map plus a set of directories.
// Enough to exercise copy/move/resume without touching a real filesystem or the network.
class InMemoryProtocolHandler(private val scheme: String = MEM_SCHEME) : ProtocolFileHandler {

    val files = LinkedHashMap<String, ByteArray>()
    val dirs = LinkedHashSet<String>()

    // Paths whose reads should throw, to simulate a mid-copy failure (flaky network, permission, etc.).
    val failReadPaths = mutableSetOf<String>()

    // Directory paths whose listing should throw, to simulate an enumeration failure (must never look empty).
    val failListPaths = mutableSetOf<String>()
    var moveCalls = 0

    override fun canHandle(path: String): Boolean = path.startsWith(scheme)

    override suspend fun openInputStream(path: String): InputStream {
        if (path in failReadPaths) throw IOException("read failed $path")
        return ByteArrayInputStream(files[path] ?: throw IOException("missing $path"))
    }

    override suspend fun openInputStream(path: String, offset: Long): InputStream {
        if (path in failReadPaths) throw IOException("read failed $path")
        val data = files[path] ?: throw IOException("missing $path")
        return ByteArrayInputStream(data).apply { if (offset > 0) skip(offset) }
    }

    override suspend fun openOutputStream(path: String): OutputStream = memOut(path, append = false)

    override suspend fun openOutputStream(path: String, offset: Long): OutputStream = memOut(path, append = offset > 0)

    private fun memOut(path: String, append: Boolean): OutputStream = object : ByteArrayOutputStream() {
        init {
            if (append) files[path]?.let { write(it) }
        }

        override fun close() {
            files[path] = toByteArray()
        }
    }

    override suspend fun sizeOf(path: String): Long = files[path]?.size?.toLong() ?: -1L

    override suspend fun listFiles(path: String): List<FileItem> {
        if (path.trimEnd('/') in failListPaths) throw IOException("listing failed $path")
        val prefix = path.trimEnd('/') + "/"
        val childFiles = files.keys
            .filter { it.startsWith(prefix) && !it.removePrefix(prefix).contains('/') }
            .map { memFileItem(it, files.getValue(it).size.toLong()) }
        val childDirs = dirs
            .filter { it.startsWith(prefix) && it != prefix.trimEnd('/') && !it.removePrefix(prefix).contains('/') }
            .map { memFileItem(it, 0L, isDirectory = true) }
        return childFiles + childDirs
    }

    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean {
        moveCalls += 1
        val target = "${destinationDir.trimEnd('/')}/${fileItem.name}"
        val data = files.remove(fileItem.path)
        if (data != null) {
            files[target] = data
            return true
        }
        // Directory subtree move (the same-volume rename fast path for a folder).
        val srcPrefix = fileItem.path.trimEnd('/') + "/"
        val moved = files.keys.filter { it.startsWith(srcPrefix) }.toList()
        if (moved.isEmpty() && fileItem.path.trimEnd('/') !in dirs) return false
        moved.forEach { key -> files["$target/${key.removePrefix(srcPrefix)}"] = files.remove(key)!! }
        dirs.remove(fileItem.path.trimEnd('/'))
        dirs.add(target.trimEnd('/'))
        return true
    }

    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean = false

    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean {
        val data = files.remove(fileItem.path) ?: return false
        val parent = fileItem.path.trimEnd('/').substringBeforeLast('/')
        files["$parent/$newName"] = data
        return true
    }

    override suspend fun createFolder(parentDir: String, name: String): Boolean =
        dirs.add("${parentDir.trimEnd('/')}/$name")

    override suspend fun deleteFile(fileItem: FileItem): Boolean {
        val srcPrefix = fileItem.path.trimEnd('/') + "/"
        files.keys.filter { it.startsWith(srcPrefix) }.toList().forEach { files.remove(it) }
        dirs.remove(fileItem.path.trimEnd('/'))
        return files.remove(fileItem.path) != null || dirs.isEmpty()
    }
}
