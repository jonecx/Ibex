package com.jonecx.ibex.di

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jonecx.ibex.ui.player.PlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PlayerModule::class],
)
object FakePlayerModule {

    @Provides
    @Singleton
    fun providePlayerFactory(@ApplicationContext context: Context): PlayerFactory {
        return object : PlayerFactory {
            override fun create(): Player {
                return ExoPlayer.Builder(context).build()
            }
        }
    }
}
