package com.jonecx.ibex.di

import com.jonecx.ibex.ui.player.ExoPlayerFactory
import com.jonecx.ibex.ui.player.PlayerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    abstract fun bindPlayerFactory(
        impl: ExoPlayerFactory,
    ): PlayerFactory
}
