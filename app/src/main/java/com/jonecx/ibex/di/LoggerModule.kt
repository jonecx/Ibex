package com.jonecx.ibex.di

import com.jonecx.ibex.logging.AppLogger
import com.jonecx.ibex.logging.TimberLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {

    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: TimberLogger): AppLogger
}
