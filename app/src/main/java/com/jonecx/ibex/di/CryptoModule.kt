package com.jonecx.ibex.di

import com.jonecx.ibex.data.crypto.CryptoManager
import com.jonecx.ibex.data.crypto.TinkCryptoManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CryptoModule {

    @Binds
    @Singleton
    abstract fun bindCryptoManager(
        impl: TinkCryptoManager,
    ): CryptoManager
}
