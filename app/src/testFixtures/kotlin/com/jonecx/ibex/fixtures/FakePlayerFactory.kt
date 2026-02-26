package com.jonecx.ibex.fixtures

import androidx.media3.common.Player
import com.jonecx.ibex.ui.player.PlayerFactory

class FakePlayerFactory(
    private val playerProvider: (() -> Player)? = null,
) : PlayerFactory {

    var createCallCount = 0
        private set

    override fun create(): Player {
        createCallCount++
        return playerProvider?.invoke()
            ?: error("FakePlayerFactory has no playerProvider configured")
    }
}
