package com.jonecx.ibex.ui.explorer.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.data.repository.SmbContextProvider
import com.jonecx.ibex.util.FileTypeUtils
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer
import okio.buffer
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SmbThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val smbContextProvider: SmbContextProvider,
    private val cacheDir: File,
) : Fetcher {

    private val smbUrl: String = uri.toString()

    private val isFullSize: Boolean =
        options.parameters.value<String>(DefaultFileImageRequestFactory.FULL_SIZE_KEY) ==
            DefaultFileImageRequestFactory.FULL_SIZE_VALUE

    override suspend fun fetch(): FetchResult? = concurrencyLimiter.withPermit {
        val fileType = FileTypeUtils.getFileTypeFromName(smbUrl)
        if (!fileType.isViewable) return@withPermit null

        val host = uri.host ?: return@withPermit null
        val cifsContext = smbContextProvider.get(host) ?: return@withPermit null

        if (isFullSize && fileType == FileType.IMAGE) {
            return@withPermit streamFullImage(cifsContext)
        }

        val cacheKey = SmbContextProvider.smbCacheKey(smbUrl)
        val cachedFile = File(cacheDir, "$cacheKey.jpg")

        if (cachedFile.exists() && cachedFile.length() > 0) {
            Log.d(TAG, "Cache hit: ${uri.lastPathSegment}")
            return@withPermit fileToResult(cachedFile)
        }

        val tempInput = File(cacheDir, "tmp_$cacheKey")
        try {
            when (fileType) {
                FileType.IMAGE -> generateImageThumbnail(cifsContext, tempInput, cachedFile)
                FileType.VIDEO -> generateVideoThumbnail(cifsContext, tempInput, cachedFile)
                else -> return@withPermit null
            }
            if (cachedFile.exists() && cachedFile.length() > 0) {
                fileToResult(cachedFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Thumbnail failed: ${uri.lastPathSegment}", e)
            cachedFile.delete()
            null
        } finally {
            tempInput.delete()
        }
    }

    private fun streamFullImage(cifsContext: CIFSContext): FetchResult {
        Log.d(TAG, "Full image stream: ${uri.lastPathSegment}")
        val cacheKey = SmbContextProvider.smbCacheKey(smbUrl)
        val tempFile = File(cacheDir, "full_$cacheKey")
        val smbFile = SmbFile(smbUrl, cifsContext)
        smbFile.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                val buf = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } != -1) {
                    output.write(buf, 0, bytesRead)
                }
            }
        }
        val mimeType = FileTypeUtils.getMimeTypeFromName(smbUrl) ?: JPEG_MIME_TYPE
        return SourceResult(
            source = ImageSource(tempFile.source().buffer(), options.context),
            mimeType = mimeType,
            dataSource = DataSource.NETWORK,
        )
    }

    private fun generateImageThumbnail(cifsContext: CIFSContext, tempInput: File, cachedFile: File) {
        downloadFromSmb(cifsContext, tempInput, EXIF_DOWNLOAD_LIMIT_BYTES)
        if (tryExifThumbnail(tempInput, cachedFile)) {
            Log.d(TAG, "EXIF thumbnail: ${uri.lastPathSegment}")
            return
        }

        Log.d(TAG, "EXIF failed, falling back to FFmpeg: ${uri.lastPathSegment}")
        tempInput.delete()
        downloadFromSmb(cifsContext, tempInput, Long.MAX_VALUE)
        generateFfmpegThumbnail(tempInput, cachedFile, isVideo = false)
    }

    private fun generateVideoThumbnail(cifsContext: CIFSContext, tempInput: File, cachedFile: File) {
        Log.d(TAG, "FFmpeg video thumbnail: ${uri.lastPathSegment}")
        downloadFromSmb(cifsContext, tempInput, VIDEO_HEAD_BYTES)
        try {
            generateFfmpegThumbnail(tempInput, cachedFile, isVideo = true)
            return
        } catch (_: IOException) {
            Log.d(TAG, "Moov not in head, reconstructing MP4: ${uri.lastPathSegment}")
        }

        val fileSize = SmbFile(smbUrl, cifsContext).length()

        if (fileSize <= VIDEO_HEAD_BYTES + MOOV_SEARCH_BYTES) {
            tempInput.delete()
            downloadFromSmb(cifsContext, tempInput, Long.MAX_VALUE)
        } else {
            val moov = findAtomInRemoteFile(cifsContext, fileSize, MOOV_TYPE)
                ?: throw IOException("No moov atom in file")
            patchMdatSizeInHead(tempInput)
            appendAtomFromRemoteFile(cifsContext, tempInput, moov)
            Log.d(TAG, "Reconstructed: head ${VIDEO_HEAD_BYTES / 1024}KB + moov ${moov.second / 1024}KB")
        }
        generateFfmpegThumbnail(tempInput, cachedFile, isVideo = true)
    }

    private fun findAtomInRemoteFile(
        cifsContext: CIFSContext,
        fileSize: Long,
        targetType: String,
    ): Pair<Long, Long>? {
        val raf = SmbRandomAccessFile(SmbFile(smbUrl, cifsContext), "r")
        try {
            var position = 0L
            val header = ByteArray(16)
            while (position < fileSize - 8) {
                raf.seek(position)
                raf.read(header, 0, 8)

                var atomSize = readBE32(header, 0)
                val type = String(header, 4, 4, Charsets.US_ASCII)

                if (atomSize == 1L) {
                    raf.read(header, 8, 8)
                    atomSize = readBE64(header, 8)
                } else if (atomSize == 0L) {
                    atomSize = fileSize - position
                }

                if (type == targetType) return Pair(position, atomSize)
                if (atomSize < 8) break
                position += atomSize
            }
            return null
        } finally {
            raf.close()
        }
    }

    private fun patchMdatSizeInHead(headFile: File) {
        val raf = java.io.RandomAccessFile(headFile, "rw")
        try {
            var position = 0L
            val header = ByteArray(8)
            while (position < raf.length() - 8) {
                raf.seek(position)
                raf.readFully(header)

                var atomSize = readBE32(header, 0)
                val type = String(header, 4, 4, Charsets.US_ASCII)

                if (atomSize == 1L && position + 16 <= raf.length()) {
                    val ext = ByteArray(8)
                    raf.readFully(ext)
                    atomSize = readBE64(ext, 0)
                }

                if (type == MDAT_TYPE) {
                    val newSize = (raf.length() - position).toInt()
                    raf.seek(position)
                    raf.write(
                        byteArrayOf(
                            (newSize shr 24 and 0xFF).toByte(),
                            (newSize shr 16 and 0xFF).toByte(),
                            (newSize shr 8 and 0xFF).toByte(),
                            (newSize and 0xFF).toByte(),
                        ),
                    )
                    return
                }

                if (atomSize < 8) break
                position += atomSize
            }
            throw IOException("mdat atom not found in head")
        } finally {
            raf.close()
        }
    }

    private fun appendAtomFromRemoteFile(
        cifsContext: CIFSContext,
        dest: File,
        atom: Pair<Long, Long>,
    ) {
        val (offset, size) = atom
        FileOutputStream(dest, true).use { output ->
            val raf = SmbRandomAccessFile(SmbFile(smbUrl, cifsContext), "r")
            try {
                raf.seek(offset)
                val buf = ByteArray(BUFFER_SIZE)
                var remaining = size
                while (remaining > 0) {
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val bytesRead = raf.read(buf, 0, toRead)
                    if (bytesRead <= 0) break
                    output.write(buf, 0, bytesRead)
                    remaining -= bytesRead
                }
            } finally {
                raf.close()
            }
        }
    }

    private fun readBE32(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 24) or
            ((data[off + 1].toLong() and 0xFF) shl 16) or
            ((data[off + 2].toLong() and 0xFF) shl 8) or
            (data[off + 3].toLong() and 0xFF)

    private fun readBE64(data: ByteArray, off: Int): Long =
        ((data[off].toLong() and 0xFF) shl 56) or
            ((data[off + 1].toLong() and 0xFF) shl 48) or
            ((data[off + 2].toLong() and 0xFF) shl 40) or
            ((data[off + 3].toLong() and 0xFF) shl 32) or
            ((data[off + 4].toLong() and 0xFF) shl 24) or
            ((data[off + 5].toLong() and 0xFF) shl 16) or
            ((data[off + 6].toLong() and 0xFF) shl 8) or
            (data[off + 7].toLong() and 0xFF)

    private fun tryExifThumbnail(input: File, output: File): Boolean {
        return try {
            val exif = ExifInterface(input.absolutePath)
            val thumbnailBytes = exif.thumbnailBytes ?: return false
            val bitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
                ?: return false

            val size = minOf(bitmap.width, bitmap.height)
            val x = (bitmap.width - size) / 2
            val y = (bitmap.height - size) / 2
            val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
            val scaled = Bitmap.createScaledBitmap(cropped, THUMBNAIL_SIZE_PX, THUMBNAIL_SIZE_PX, true)

            output.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            if (bitmap !== cropped) bitmap.recycle()
            if (cropped !== scaled) cropped.recycle()
            scaled.recycle()

            output.exists() && output.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadFromSmb(cifsContext: CIFSContext, dest: File, limit: Long) {
        val smbFile = SmbFile(smbUrl, cifsContext)
        smbFile.inputStream.use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(BUFFER_SIZE)
                var totalRead = 0L
                var bytesRead: Int
                while (input.read(buf).also { bytesRead = it } != -1 && totalRead < limit) {
                    output.write(buf, 0, bytesRead)
                    totalRead += bytesRead
                }
            }
        }
    }

    private fun generateFfmpegThumbnail(input: File, output: File, isVideo: Boolean) {
        val command = if (isVideo) {
            "-y -i ${input.absolutePath} -ss 00:00:01 -vframes 1 " +
                "-vf scale=$THUMBNAIL_SIZE_PX:-1 -q:v 4 ${output.absolutePath}"
        } else {
            "-y -i ${input.absolutePath} " +
                "-vf scale=$THUMBNAIL_SIZE_PX:-1 -q:v 4 ${output.absolutePath}"
        }
        val session = FFmpegKit.execute(command)

        if (!ReturnCode.isSuccess(session.returnCode) || !output.exists()) {
            if (isVideo) {
                val retry = "-y -i ${input.absolutePath} -vframes 1 " +
                    "-vf scale=$THUMBNAIL_SIZE_PX:-1 -q:v 4 ${output.absolutePath}"
                val retrySession = FFmpegKit.execute(retry)
                if (!ReturnCode.isSuccess(retrySession.returnCode) || !output.exists()) {
                    throw IOException("FFmpeg thumbnail generation failed")
                }
            } else {
                throw IOException("FFmpeg thumbnail generation failed")
            }
        }
    }

    private fun fileToResult(file: File): FetchResult {
        val buffer = Buffer()
        file.source().buffer().use { source ->
            buffer.writeAll(source)
        }
        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = JPEG_MIME_TYPE,
            dataSource = DataSource.DISK,
        )
    }

    companion object {
        private const val THUMBNAIL_SIZE_PX = 200
        private const val EXIF_DOWNLOAD_LIMIT_BYTES = 256L * 1024
        private const val VIDEO_HEAD_BYTES = 1L * 1024 * 1024
        private const val MOOV_SEARCH_BYTES = 2L * 1024 * 1024
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_CONCURRENT_THUMBNAILS = 4
        private const val JPEG_QUALITY = 80
        private const val JPEG_MIME_TYPE = "image/jpeg"
        private const val MOOV_TYPE = "moov"
        private const val MDAT_TYPE = "mdat"
        private const val TAG = "SmbThumbnail"
        private val concurrencyLimiter = Semaphore(MAX_CONCURRENT_THUMBNAILS)
    }
}

class SmbFetcherFactory(
    private val smbContextProvider: SmbContextProvider,
    private val cacheDir: File,
) : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (data.scheme != SMB_SCHEME) return null
        return SmbThumbnailFetcher(data, options, smbContextProvider, cacheDir)
    }

    companion object {
        private const val SMB_SCHEME = "smb"
    }
}
