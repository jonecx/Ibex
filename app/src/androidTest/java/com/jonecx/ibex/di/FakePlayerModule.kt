package com.jonecx.ibex.di

import com.jonecx.ibex.ui.player.ExoPlayerFactory
import com.jonecx.ibex.ui.player.PlayerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PlayerModule::class],
)
abstract class FakePlayerModule {

    @Binds
    abstract fun bindPlayerFactory(
        impl: ExoPlayerFactory,
    ): PlayerFactory
}
