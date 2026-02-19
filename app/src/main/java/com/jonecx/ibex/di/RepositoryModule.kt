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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFileRepositoryFactory(
        @ApplicationContext context: Context,
    ): FileRepositoryFactory {
        return FileRepositoryFactory(context)
    }
}

class FileRepositoryFactory(
    private val context: Context,
) {
    fun createLocalFileRepository(): FileRepository = LocalFileRepository(context)

    fun createMediaFileRepository(mediaType: MediaType): FileRepository =
        MediaFileRepository(context, mediaType)

    fun createAppsRepository(): FileRepository = AppsRepository(context)

    fun createRecentFilesRepository(): FileRepository = RecentFilesRepository(context)

    fun createTrashRepository(): FileRepository = TrashRepository(context)
}
