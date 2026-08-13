package com.jonecx.ibex.di

import android.content.Context
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.repository.AppsRepository
import com.jonecx.ibex.data.repository.CompositeFileMoveManager
import com.jonecx.ibex.data.repository.DefaultFileClipboardManager
import com.jonecx.ibex.data.repository.FileClipboardManager
import com.jonecx.ibex.data.repository.FileMoveManager
import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.FileSystemMoveManager
import com.jonecx.ibex.data.repository.FileTrashManager
import com.jonecx.ibex.data.repository.HomeSourceStatsRepository
import com.jonecx.ibex.data.repository.LocalFileRepository
import com.jonecx.ibex.data.repository.MediaFileRepository
import com.jonecx.ibex.data.repository.MediaStoreFileTrashManager
import com.jonecx.ibex.data.repository.MediaStoreHomeSourceStatsRepository
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.data.repository.ProtocolFileHandler
import com.jonecx.ibex.data.repository.RecentFilesRepository
import com.jonecx.ibex.data.repository.SmbContextProvider
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.data.repository.SmbFileMoveManager
import com.jonecx.ibex.data.repository.SmbFileRepository
import com.jonecx.ibex.data.repository.TrashRepository
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

interface FileRepositoryFactory {
    fun createLocalFileRepository(): FileRepository
    fun createMediaFileRepository(mediaType: MediaType): FileRepository
    fun createAppsRepository(): FileRepository
    fun createRecentFilesRepository(): FileRepository
    fun createTrashRepository(): FileRepository
    fun createSmbFileRepository(connectionId: String): FileRepository
}

class RealFileRepositoryFactory(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
    private val networkPreferences: NetworkConnectionsPreferencesContract,
    private val smbContextProvider: SmbContextProviderContract,
) : FileRepositoryFactory {
    override fun createLocalFileRepository(): FileRepository = LocalFileRepository(context, ioDispatcher)

    override fun createMediaFileRepository(mediaType: MediaType): FileRepository =
        MediaFileRepository(context, mediaType, ioDispatcher)

    override fun createAppsRepository(): FileRepository = AppsRepository(context, ioDispatcher)

    override fun createRecentFilesRepository(): FileRepository = RecentFilesRepository(context, ioDispatcher)

    override fun createTrashRepository(): FileRepository = TrashRepository(context, ioDispatcher)

    override fun createSmbFileRepository(connectionId: String): FileRepository =
        SmbFileRepository(connectionId, networkPreferences, ioDispatcher, smbContextProvider)
}

val repositoryModule = module {
    single<FileRepositoryFactory> {
        RealFileRepositoryFactory(androidContext(), get(IoDispatcher), get(), get())
    }
    single<FileTrashManager> { MediaStoreFileTrashManager(androidContext(), get(IoDispatcher)) }

    // ProtocolFileHandler multibinding: each handler is collected via getAll() below.
    single { FileSystemMoveManager(get(IoDispatcher)) } bind ProtocolFileHandler::class
    single { SmbFileMoveManager(get(), get(IoDispatcher)) } bind ProtocolFileHandler::class
    single<FileMoveManager> {
        CompositeFileMoveManager(getAll<ProtocolFileHandler>().toSet(), get(IoDispatcher))
    }

    single<FileClipboardManager> { DefaultFileClipboardManager(get()) }
    single<SmbContextProviderContract> { SmbContextProvider() }

    single<HomeSourceStatsRepository> {
        MediaStoreHomeSourceStatsRepository(androidContext(), get(IoDispatcher))
    }
}
