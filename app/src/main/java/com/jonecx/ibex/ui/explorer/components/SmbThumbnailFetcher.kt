package com.jonecx.ibex.ui.explorer.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.formatFileSize
import jcifs.CIFSContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class SmbThumbnailFetcher(
    private val uri: Uri,
    private val options: Options,
    private val smbContextProvider: SmbContextProviderContract,
    private val cacheDir: File,
) : Fetcher {

    private val smbUrl: String = uri.toString()

    private val isFullSize: Boolean = options.isFullSizeRequest()

    override suspend fun fetch(): FetchResult? = concurrencyLimiter.withPermit {
        val fileType = FileTypeUtils.getFileTypeFromName(smbUrl)
        if (!fileType.isViewable) return@withPermit null

        val host = uri.host ?: return@withPermit null
        val cifsContext = smbContextProvider.get(host) ?: return@withPermit null

        if (isFullSize && fileType.isImage) {
            return@withPermit streamFullImage(cifsContext)
        }

        val cacheKey = SmbContextProviderContract.smbCacheKey(smbUrl)
        val cachedFile = File(cacheDir, "$cacheKey.jpg")

        if (cachedFile.isValidCache()) {
            Timber.d("Cache hit: %s", uri.lastPathSegment)
            return@withPermit fileToResult(cachedFile)
        }

        try {
            when {
                fileType.isImage -> generateImageThumbnail(cifsContext, cachedFile)
                fileType.isVideo -> generateVideoThumbnail(cifsContext, cachedFile)
                else -> return@withPermit null
            }
            if (cachedFile.isValidCache()) {
                fileToResult(cachedFile)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Thumbnail failed: %s", uri.lastPathSegment)
            cachedFile.delete()
            null
        }
    }

    private fun streamFullImage(cifsContext: CIFSContext): FetchResult {
        Timber.d("Full image stream: %s", uri.lastPathSegment)
        val cacheKey = SmbContextProviderContract.smbCacheKey(smbUrl)
        val tempFile = File(cacheDir, "full_$cacheKey")
        val smbFile = SmbFile(smbUrl, cifsContext)
        smbFile.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output, bufferSize = FileTypeUtils.IO_BUFFER_SIZE)
            }
        }
        val mimeType = FileTypeUtils.getMimeTypeFromName(smbUrl) ?: FileTypeUtils.JPEG_MIME_TYPE
        return SourceResult(
            source = ImageSource(tempFile.source().buffer(), options.context),
            mimeType = mimeType,
            dataSource = DataSource.NETWORK,
        )
    }

    private fun generateImageThumbnail(cifsContext: CIFSContext, cachedFile: File) {
        Timber.d("Fetching image thumbnail: %s", uri.lastPathSegment)

        val mimeType = FileTypeUtils.getMimeTypeFromName(smbUrl)
        if (mimeType?.startsWith(FileTypeUtils.JPEG_MIME_TYPE) == true) {
            val exifBitmap = tryExifThumbnail(cifsContext)
            if (exifBitmap != null) {
                Timber.d("EXIF thumbnail: %s", uri.lastPathSegment)
                saveThumbnail(exifBitmap, cachedFile)
                return
            }
        }

        Timber.d("Downloading full image for thumbnail: %s", uri.lastPathSegment)
        val bitmap = decodeFullImage(cifsContext)
        if (bitmap != null) {
            saveThumbnail(bitmap, cachedFile)
            return
        }

        throw IOException("Could not decode image: ${uri.lastPathSegment}")
    }

    private fun tryExifThumbnail(cifsContext: CIFSContext): Bitmap? {
        for (headerSize in EXIF_HEADER_SIZES) {
            try {
                val headerData = readPartialFromSmb(cifsContext, headerSize)
                    ?: continue

                Timber.d("Read %s for EXIF extraction", formatFileSize(headerData.size.toLong()))

                val exif = ExifInterface(ByteArrayInputStream(headerData))
                val thumbnailBytes = exif.thumbnailBytes ?: continue
                val bitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)

                if (bitmap != null) {
                    Timber.d("Found EXIF thumbnail: %dx%d (from %s)", bitmap.width, bitmap.height, formatFileSize(headerSize.toLong()))
                    return bitmap
                }
            } catch (_: Exception) {
                Timber.v("No EXIF thumbnail at %s", formatFileSize(headerSize.toLong()))
            }
        }

        Timber.d("No EXIF thumbnail found for %s", uri.lastPathSegment)
        return null
    }

    private fun decodeFullImage(cifsContext: CIFSContext): Bitmap? {
        return try {
            val smbFile = SmbFile(smbUrl, cifsContext)
            val fileSizeLong = smbFile.length()
            if (fileSizeLong <= 0 || fileSizeLong > MAX_DECODE_FILE_SIZE) return null
            val fileSize = fileSizeLong.toInt()

            Timber.d("Image file size: %s", formatFileSize(fileSize.toLong()))

            val imageData = readPartialFromSmb(cifsContext, fileSize) ?: return null

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, boundsOptions)

            if (!hasValidDimensions(boundsOptions.outWidth, boundsOptions.outHeight)) {
                Timber.e("Invalid image dimensions: %dx%d", boundsOptions.outWidth, boundsOptions.outHeight)
                return null
            }

            val sampleSize = calculateSampleSize(boundsOptions.outWidth, boundsOptions.outHeight)

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, decodeOptions)

            if (bitmap != null) {
                Timber.d(
                    "Decoded image: %dx%d (sample=%d from %dx%d)",
                    bitmap.width,
                    bitmap.height,
                    sampleSize,
                    boundsOptions.outWidth,
                    boundsOptions.outHeight,
                )
            }

            bitmap
        } catch (e: Exception) {
            Timber.e(e, "Error decoding full image")
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > FileTypeUtils.THUMBNAIL_SIZE_PX * SAMPLE_SIZE_THRESHOLD_FACTOR || height / sampleSize > FileTypeUtils.THUMBNAIL_SIZE_PX * SAMPLE_SIZE_THRESHOLD_FACTOR) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun generateVideoThumbnail(cifsContext: CIFSContext, cachedFile: File) {
        Timber.d("Fetching video thumbnail via streaming: %s", uri.lastPathSegment)

        val mediaDataSource = SmbVideoThumbnailDataSource(smbUrl, cifsContext)
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(mediaDataSource)

            val embeddedPicture = retriever.embeddedPicture
            var bitmap = if (embeddedPicture != null) {
                Timber.d("Found embedded thumbnail in video metadata")
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else {
                null
            }

            if (bitmap == null) {
                bitmap = retriever.getFrameAtTime(
                    FileTypeUtils.VIDEO_FRAME_TIME_US,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                )
            }

            if (bitmap == null) {
                Timber.d("No frame at 1s, trying first frame")
                bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }

            if (bitmap != null) {
                saveThumbnail(bitmap, cachedFile)
                Timber.d("Video thumbnail created: %s", uri.lastPathSegment)
                return
            }

            throw IOException("Could not extract video frame: ${uri.lastPathSegment}")
        } catch (e: Exception) {
            Timber.e(e, "Error extracting video frame")
            throw IOException("Cannot generate thumbnail for ${uri.lastPathSegment}", e)
        } finally {
            retriever.release()
            mediaDataSource.close()
        }
    }

    private fun readPartialFromSmb(cifsContext: CIFSContext, bytesToRead: Int): ByteArray? {
        return try {
            val smbFile = SmbFile(smbUrl, cifsContext)
            val buffer = ByteArray(bytesToRead)
            smbFile.inputStream.use { input ->
                var totalRead = 0
                while (totalRead < bytesToRead) {
                    val read = input.read(buffer, totalRead, bytesToRead - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
            }
            buffer
        } catch (e: Exception) {
            Timber.e(e, "Error reading partial SMB file")
            null
        }
    }

    private fun saveThumbnail(source: Bitmap, cacheFile: File) {
        val scaled = createSquareThumbnail(source)
        cacheFile.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, FileTypeUtils.JPEG_QUALITY, out)
        }
        if (scaled !== source) scaled.recycle()
        source.recycle()
    }

    private fun createSquareThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scale = maxOf(FileTypeUtils.THUMBNAIL_SIZE_PX.toFloat() / width, FileTypeUtils.THUMBNAIL_SIZE_PX.toFloat() / height)
        val scaledWidth = (width * scale).toInt().coerceAtLeast(FileTypeUtils.THUMBNAIL_SIZE_PX)
        val scaledHeight = (height * scale).toInt().coerceAtLeast(FileTypeUtils.THUMBNAIL_SIZE_PX)

        val scaled = if (scaledWidth != width || scaledHeight != height) {
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap
        }

        val cropX = (scaled.width - FileTypeUtils.THUMBNAIL_SIZE_PX) / 2
        val cropY = (scaled.height - FileTypeUtils.THUMBNAIL_SIZE_PX) / 2
        val cropped = Bitmap.createBitmap(scaled, cropX, cropY, FileTypeUtils.THUMBNAIL_SIZE_PX, FileTypeUtils.THUMBNAIL_SIZE_PX)

        if (scaled !== bitmap && scaled !== cropped) {
            scaled.recycle()
        }

        return cropped
    }

    private fun fileToResult(file: File): FetchResult {
        val buffer = Buffer()
        file.source().buffer().use { source ->
            buffer.writeAll(source)
        }
        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = FileTypeUtils.JPEG_MIME_TYPE,
            dataSource = DataSource.DISK,
        )
    }

    private fun File.isValidCache(): Boolean = exists() && length() > 0

    private fun hasValidDimensions(width: Int, height: Int): Boolean = width > 0 && height > 0

    companion object {
        private const val SAMPLE_SIZE_THRESHOLD_FACTOR = 2
        private const val MAX_DECODE_FILE_SIZE = 50L * 1024 * 1024
        private const val MAX_CONCURRENT_THUMBNAILS = 4
        private val EXIF_HEADER_SIZES = listOf(64 * 1024, 128 * 1024, 256 * 1024)
        private val concurrencyLimiter = Semaphore(MAX_CONCURRENT_THUMBNAILS)
    }
}

class SmbFetcherFactory(
    private val smbContextProvider: SmbContextProviderContract,
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
