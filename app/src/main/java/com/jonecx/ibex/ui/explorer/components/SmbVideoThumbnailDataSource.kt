package com.jonecx.ibex.ui.explorer.components

import android.media.MediaDataSource
import com.jonecx.ibex.util.formatFileSize
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import timber.log.Timber

/**
 * MediaDataSource that reads directly from SMB with full seeking support.
 * This allows MediaMetadataRetriever to extract frames from any position
 * in the video without downloading the entire file.
 */
class SmbVideoThumbnailDataSource(
    private val smbUrl: String,
    private val cifsContext: CIFSContext,
) : MediaDataSource() {

    private var randomAccess: SmbRandomAccessFile? = null
    private var fileSize: Long = -1

    @Synchronized
    private fun ensureOpen() {
        if (randomAccess != null) return

        val smbFile = SmbFile(smbUrl, cifsContext)
        fileSize = smbFile.length()
        randomAccess = smbFile.openRandomAccess("r")

        Timber.d("SMB MediaDataSource opened, size: %s", formatFileSize(fileSize))
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        ensureOpen()

        val raf = randomAccess ?: return -1

        if (position >= fileSize) {
            return -1
        }

        synchronized(this) {
            raf.seek(position)
            val bytesToRead = minOf(size.toLong(), fileSize - position).toInt()
            return raf.read(buffer, offset, bytesToRead)
        }
    }

    @Synchronized
    override fun getSize(): Long {
        ensureOpen()
        return fileSize
    }

    override fun close() {
        Timber.d("Closing SMB MediaDataSource")
        try {
            randomAccess?.close()
        } catch (e: Exception) {
            Timber.w(e, "Error closing random access")
        }
        randomAccess = null
    }
}
