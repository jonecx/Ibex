package com.jonecx.ibex.fixtures

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.lang.reflect.Proxy

class FakePlayerFactoryTest {

    private val stubPlayer: Player = Proxy.newProxyInstance(
        Player::class.java.classLoader,
        arrayOf(Player::class.java),
    ) { _, _, _ -> null } as Player

    @Test
    fun `create increments call count`() {
        val factory = FakePlayerFactory(playerProvider = { stubPlayer })

        assertEquals(0, factory.createCallCount)
        factory.create()
        assertEquals(1, factory.createCallCount)
        factory.create()
        assertEquals(2, factory.createCallCount)
    }

    @Test
    fun `create returns player from provider`() {
        val factory = FakePlayerFactory(playerProvider = { stubPlayer })

        assertSame(stubPlayer, factory.create())
    }

    @Test(expected = IllegalStateException::class)
    fun `create throws when no provider configured`() {
        val factory = FakePlayerFactory()
        factory.create()
    }
}
