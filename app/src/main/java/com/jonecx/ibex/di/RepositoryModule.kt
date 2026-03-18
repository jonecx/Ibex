package com.jonecx.ibex.di

import android.content.Context
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.data.repository.AppsRepository
import com.jonecx.ibex.data.repository.DefaultFileClipboardManager
import com.jonecx.ibex.data.repository.FileClipboardManager
import com.jonecx.ibex.data.repository.FileMoveManager
import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.FileSystemMoveManager
import com.jonecx.ibex.data.repository.FileTrashManager
import com.jonecx.ibex.data.repository.LocalFileRepository
import com.jonecx.ibex.data.repository.MediaFileRepository
import com.jonecx.ibex.data.repository.MediaStoreFileTrashManager
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.data.repository.RecentFilesRepository
import com.jonecx.ibex.data.repository.SmbContextProvider
import com.jonecx.ibex.data.repository.SmbContextProviderContract
import com.jonecx.ibex.data.repository.SmbFileRepository
import com.jonecx.ibex.data.repository.TrashRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Singleton

interface FileRepositoryFactory {
    fun createLocalFileRepository(): FileRepository
    fun createMediaFileRepository(mediaType: MediaType): FileRepository
    fun createAppsRepository(): FileRepository
    fun createRecentFilesRepository(): FileRepository
    fun createTrashRepository(): FileRepository
    fun createSmbFileRepository(connectionId: String): FileRepository
}

@Singleton
class RealFileRepositoryFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFileRepositoryFactory(
        impl: RealFileRepositoryFactory,
    ): FileRepositoryFactory

    @Binds
    @Singleton
    abstract fun bindFileTrashManager(
        impl: MediaStoreFileTrashManager,
    ): FileTrashManager

    @Binds
    @Singleton
    abstract fun bindFileMoveManager(
        impl: FileSystemMoveManager,
    ): FileMoveManager

    @Binds
    @Singleton
    abstract fun bindFileClipboardManager(
        impl: DefaultFileClipboardManager,
    ): FileClipboardManager

    @Binds
    @Singleton
    abstract fun bindSmbContextProvider(
        impl: SmbContextProvider,
    ): SmbContextProviderContract
}
