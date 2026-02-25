package com.jonecx.ibex.di

import android.content.Context
import coil.ImageLoader
import com.jonecx.ibex.util.FakeImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ImageLoaderModule::class],
)
object FakeImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return FakeImageLoader(context)
    }
}
