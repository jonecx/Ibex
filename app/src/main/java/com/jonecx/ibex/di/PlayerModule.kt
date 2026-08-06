package com.jonecx.ibex.di

import com.jonecx.ibex.ui.player.ExoPlayerFactory
import com.jonecx.ibex.ui.player.PlayerFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playerModule = module {
    single<PlayerFactory> { ExoPlayerFactory(androidContext(), get()) }
}
