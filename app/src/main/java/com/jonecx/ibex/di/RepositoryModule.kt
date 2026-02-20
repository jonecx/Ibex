package com.jonecx.ibex.di

import android.content.Context
import com.jonecx.ibex.data.repository.AppsRepository
import com.jonecx.ibex.data.repository.FileRepository
import com.jonecx.ibex.data.repository.LocalFileRepository
import com.jonecx.ibex.data.repository.MediaFileRepository
import com.jonecx.ibex.data.repository.MediaType
import com.jonecx.ibex.data.repository.RecentFilesRepository
import com.jonecx.ibex.data.repository.TrashRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

interface FileRepositoryFactory {
    fun createLocalFileRepository(): FileRepository
    fun createMediaFileRepository(mediaType: MediaType): FileRepository
    fun createAppsRepository(): FileRepository
    fun createRecentFilesRepository(): FileRepository
    fun createTrashRepository(): FileRepository
}

class RealFileRepositoryFactory(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : FileRepositoryFactory {
    override fun createLocalFileRepository(): FileRepository = LocalFileRepository(context, ioDispatcher)

    override fun createMediaFileRepository(mediaType: MediaType): FileRepository =
        MediaFileRepository(context, mediaType, ioDispatcher)

    override fun createAppsRepository(): FileRepository = AppsRepository(context, ioDispatcher)

    override fun createRecentFilesRepository(): FileRepository = RecentFilesRepository(context, ioDispatcher)

    override fun createTrashRepository(): FileRepository = TrashRepository(context, ioDispatcher)
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFileRepositoryFactory(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): FileRepositoryFactory {
        return RealFileRepositoryFactory(context, ioDispatcher)
    }
}
