package com.jonecx.ibex.data.model

import com.jonecx.azmaree.player.model.StreamingProtocol
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoFeedTest {

    @Test
    fun `protocol defaults to HLS for m3u8 url`() {
        val feed = VideoFeed(title = "Live", url = "https://example.com/stream.m3u8")
        assertEquals(StreamingProtocol.HLS, feed.protocol)
    }

    @Test
    fun `protocol defaults to DASH for mpd url`() {
        val feed = VideoFeed(title = "Live", url = "https://example.com/stream.mpd")
        assertEquals(StreamingProtocol.DASH, feed.protocol)
    }

    @Test
    fun `protocol defaults to progressive for mp4 url`() {
        val feed = VideoFeed(title = "Clip", url = "https://example.com/clip.mp4")
        assertEquals(StreamingProtocol.PROGRESSIVE, feed.protocol)
    }

    @Test
    fun `toVideoSource maps fields and blanks become null`() {
        val feed = VideoFeed(id = "1", title = "Live", url = "https://example.com/s.m3u8")
        val source = feed.toVideoSource()

        assertEquals("1", source.id)
        assertEquals("https://example.com/s.m3u8", source.url)
        assertEquals(StreamingProtocol.HLS, source.protocol)
        assertEquals("Live", source.title)
        assertNull(source.thumbnailUrl)
        assertNull(source.description)
    }

    @Test
    fun `serialization round-trip preserves fields including explicit protocol`() {
        // Explicit protocol that disagrees with the URL heuristic, to exercise the custom serializer.
        val feed = VideoFeed(
            id = "abc",
            title = "News",
            url = "https://example.com/live",
            thumbnailUrl = "https://example.com/thumb.jpg",
            protocol = StreamingProtocol.HLS,
            description = "24/7",
        )

        val restored = Json.decodeFromString<VideoFeed>(Json.encodeToString(feed))

        assertEquals(feed, restored)
        assertEquals(StreamingProtocol.HLS, restored.protocol)
    }
}
