package com.jonecx.ibex.ui.player

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.util.FileTypeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerFactory {
    fun create(): Player
}

val LocalPlayerFactory = staticCompositionLocalOf<PlayerFactory> {
    error("No PlayerFactory provided")
}

@Singleton
class ExoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    smbContextProvider: SmbContextProviderContract,
) : PlayerFactory {

    private val dataSourceFactory = SmbAwareDataSourceFactory(
        DefaultDataSource.Factory(context),
        SmbDataSourceFactory(smbContextProvider),
    )

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun create(): Player {
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }
}

@UnstableApi
private class SmbAwareDataSourceFactory(
    private val defaultFactory: DataSource.Factory,
    private val smbFactory: SmbDataSourceFactory,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        SmbAwareDataSource(defaultFactory.createDataSource(), smbFactory.createDataSource())
}

@UnstableApi
private class SmbAwareDataSource(
    private val defaultSource: DataSource,
    private val smbSource: SmbDataSource,
) : DataSource {

    private var activeSource: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        activeSource = if (dataSpec.uri.scheme == FileTypeUtils.SMB_SCHEME) smbSource else defaultSource
        return activeSource!!.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        activeSource?.read(buffer, offset, length)
            ?: throw IllegalStateException("DataSource not opened")

    override fun getUri(): Uri? = activeSource?.uri

    override fun close() {
        activeSource?.close()
        activeSource = null
    }

    override fun addTransferListener(transferListener: TransferListener) {
        defaultSource.addTransferListener(transferListener)
        smbSource.addTransferListener(transferListener)
    }
}
