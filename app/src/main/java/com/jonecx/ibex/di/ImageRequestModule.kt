package com.jonecx.ibex.di

import com.jonecx.ibex.ui.explorer.components.DefaultFileImageRequestFactory
import com.jonecx.ibex.ui.explorer.components.FileImageRequestFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageRequestModule {

    @Binds
    abstract fun bindFileImageRequestFactory(
        impl: DefaultFileImageRequestFactory,
    ): FileImageRequestFactory
}
