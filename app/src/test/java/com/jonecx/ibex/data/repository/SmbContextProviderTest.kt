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
    fun `register and get returns stored context`() {
        val context = FakeTestCifsContext()
        provider.register("192.168.1.1", context)

        assertEquals(context, provider.get("192.168.1.1"))
    }

    @Test
    fun `register overwrites existing context for same host`() {
        val first = FakeTestCifsContext()
        val second = FakeTestCifsContext()

        provider.register("host", first)
        provider.register("host", second)

        assertEquals(second, provider.get("host"))
    }

    @Test
    fun `get returns null for different host`() {
        provider.register("host-a", FakeTestCifsContext())
        assertNull(provider.get("host-b"))
    }

    @Test
    fun `multiple hosts are stored independently`() {
        val ctxA = FakeTestCifsContext()
        val ctxB = FakeTestCifsContext()

        provider.register("host-a", ctxA)
        provider.register("host-b", ctxB)

        assertEquals(ctxA, provider.get("host-a"))
        assertEquals(ctxB, provider.get("host-b"))
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
