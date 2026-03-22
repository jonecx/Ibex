package com.jonecx.ibex.ui.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.util.FileTypeUtils
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile

@UnstableApi
class SmbDataSource(
    private val smbContextProvider: SmbContextProviderContract,
) : BaseDataSource(true) {

    private var smbRandomAccessFile: SmbRandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0L

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val path = dataSpec.uri.toString()
        val host = FileTypeUtils.smbExtractHost(path)
            ?: throw SmbDataSourceException("Cannot extract host from: $path")
        val context = smbContextProvider.get(host)
            ?: throw SmbDataSourceException("No SMB context for host: $host")

        val smbFile = SmbFile(path, context)
        val raf = smbFile.openRandomAccess("r")

        if (dataSpec.position > 0) {
            raf.seek(dataSpec.position)
        }

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            raf.length() - dataSpec.position
        }

        smbRandomAccessFile = raf
        uri = dataSpec.uri
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val raf = smbRandomAccessFile ?: return C.RESULT_END_OF_INPUT
        val toRead = minOf(length.toLong(), bytesRemaining).toInt()
        val bytesRead = raf.read(buffer, offset, toRead)

        if (bytesRead == -1) return C.RESULT_END_OF_INPUT

        bytesRemaining -= bytesRead
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try {
            smbRandomAccessFile?.close()
        } finally {
            smbRandomAccessFile = null
            uri = null
            bytesRemaining = 0L
            transferEnded()
        }
    }
}

class SmbDataSourceException(message: String) : java.io.IOException(message)

@UnstableApi
class SmbDataSourceFactory(
    private val smbContextProvider: SmbContextProviderContract,
) : DataSource.Factory {
    override fun createDataSource(): SmbDataSource = SmbDataSource(smbContextProvider)
}
