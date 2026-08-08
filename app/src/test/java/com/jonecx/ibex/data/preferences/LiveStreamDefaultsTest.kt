package com.jonecx.ibex.data.preferences

import com.jonecx.azmaree.player.model.StreamingProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStreamDefaultsTest {

    private val streams = LiveStreamDefaults.AZMAREE_STREAMS

    @Test
    fun `ships the full Azmaree list in order`() {
        assertEquals(13, streams.size)
        assertEquals("0, BBC World Service", streams.first().id)
        assertEquals("12, Sky News", streams.last().id)
    }

    @Test
    fun `ids are unique so seeding never duplicates`() {
        assertEquals(streams.size, streams.map { it.id }.toSet().size)
    }

    @Test
    fun `every stream has a title and url`() {
        assertTrue(streams.all { it.title.isNotBlank() && it.url.isNotBlank() })
    }

    @Test
    fun `angel one is the dash entry`() {
        val angelOne = streams.first { it.title.startsWith("Angel One") }
        assertEquals(StreamingProtocol.DASH, angelOne.protocol)
    }
}
