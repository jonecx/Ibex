package com.jonecx.ibex.ui.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PlayerFactory {
    fun create(): Player
}

@Singleton
class ExoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerFactory {
    override fun create(): Player {
        return ExoPlayer.Builder(context).build()
    }
}
