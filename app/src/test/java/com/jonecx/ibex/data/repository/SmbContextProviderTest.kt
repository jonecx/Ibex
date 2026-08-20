package com.jonecx.ibex.data.repository

import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmbContextProviderTest {

    private val provider = SmbContextProvider()

    @Test
    fun `get returns null for unregistered host`() {
        assertNull(provider.get("unknown-host"))
    }

    @Test
    fun `getOrCreate stores context retrievable by get`() {
        val context = provider.getOrCreate("192.168.1.1", "sig") { FakeTestCifsContext() }

        assertEquals(context, provider.get("192.168.1.1"))
    }

    @Test
    fun `get returns null for different host`() {
        provider.getOrCreate("host-a", "sig") { FakeTestCifsContext() }
        assertNull(provider.get("host-b"))
    }

    @Test
    fun `multiple hosts are stored independently`() {
        val ctxA = provider.getOrCreate("host-a", "sig") { FakeTestCifsContext() }
        val ctxB = provider.getOrCreate("host-b", "sig") { FakeTestCifsContext() }

        assertEquals(ctxA, provider.get("host-a"))
        assertEquals(ctxB, provider.get("host-b"))
    }

    @Test
    fun `getOrCreate builds once and reuses context for same signature`() {
        var builds = 0
        val first = provider.getOrCreate("host", "sig-1") {
            builds++
            FakeTestCifsContext()
        }
        val second = provider.getOrCreate("host", "sig-1") {
            builds++
            FakeTestCifsContext()
        }

        assertEquals(1, builds)
        assertEquals(first, second)
    }

    @Test
    fun `getOrCreate rebuilds when signature changes`() {
        var builds = 0
        val first = provider.getOrCreate("host", "sig-1") {
            builds++
            FakeTestCifsContext()
        }
        val second = provider.getOrCreate("host", "sig-2") {
            builds++
            FakeTestCifsContext()
        }

        assertEquals(2, builds)
        assertTrue("Different signature should yield a different context", first !== second)
        assertEquals(second, provider.get("host"))
    }

    @Test
    fun `smbCacheKey produces consistent output`() {
        val key1 = SmbContextProviderContract.smbCacheKey("smb://host/share/file.jpg")
        val key2 = SmbContextProviderContract.smbCacheKey("smb://host/share/file.jpg")
        assertEquals(key1, key2)
    }

    @Test
    fun `smbCacheKey produces non-negative value`() {
        val paths = listOf(
            "smb://host/share/file.jpg",
            "smb://192.168.1.1/data/photo.png",
            "smb://server/folder/deep/nested/video.mp4",
            "",
        )
        paths.forEach { path ->
            val key = SmbContextProviderContract.smbCacheKey(path)
            assertNotNull(key)
            assertTrue("Key should be non-negative for: $path", key.toLong() >= 0)
        }
    }

    @Test
    fun `smbCacheKey produces different keys for different paths`() {
        val key1 = SmbContextProviderContract.smbCacheKey("smb://host/share/a.jpg")
        val key2 = SmbContextProviderContract.smbCacheKey("smb://host/share/b.jpg")
        assertTrue("Different paths should produce different keys", key1 != key2)
    }
}

private fun FakeTestCifsContext(): CIFSContext =
    BaseContext(PropertyConfiguration(java.util.Properties()))
