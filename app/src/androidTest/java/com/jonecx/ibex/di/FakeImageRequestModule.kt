package com.jonecx.ibex.di

import com.jonecx.ibex.fixtures.FakeFileImageRequestFactory
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ImageRequestModule::class],
)
object FakeImageRequestModule {

    @Provides
    @Singleton
    fun provideFileImageRequestFactory(): FileImageRequestFactory {
        return FakeFileImageRequestFactory()
    }
}
