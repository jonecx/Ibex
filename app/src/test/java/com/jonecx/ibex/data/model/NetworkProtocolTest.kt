package com.jonecx.ibex.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkProtocolTest {

    @Test
    fun smbHasCorrectDefaultPort() {
        assertEquals(445, NetworkProtocol.SMB.defaultPort)
    }

    @Test
    fun ftpHasCorrectDefaultPort() {
        assertEquals(21, NetworkProtocol.FTP.defaultPort)
    }

    @Test
    fun cloudHasCorrectDefaultPort() {
        assertEquals(443, NetworkProtocol.CLOUD.defaultPort)
    }

    @Test
    fun smbUsesIpOctets() {
        assertTrue(NetworkProtocol.SMB.usesIpOctets)
    }

    @Test
    fun ftpUsesIpOctets() {
        assertTrue(NetworkProtocol.FTP.usesIpOctets)
    }

    @Test
    fun cloudDoesNotUseIpOctets() {
        assertFalse(NetworkProtocol.CLOUD.usesIpOctets)
    }

    @Test
    fun smbSupportsAnonymous() {
        assertTrue(NetworkProtocol.SMB.supportsAnonymous)
    }

    @Test
    fun ftpSupportsAnonymous() {
        assertTrue(NetworkProtocol.FTP.supportsAnonymous)
    }

    @Test
    fun cloudDoesNotSupportAnonymous() {
        assertFalse(NetworkProtocol.CLOUD.supportsAnonymous)
    }

    @Test
    fun smbHasLanIcon() {
        assertEquals(Icons.Filled.Lan, NetworkProtocol.SMB.icon)
    }

    @Test
    fun ftpHasFolderIcon() {
        assertEquals(Icons.Filled.Folder, NetworkProtocol.FTP.icon)
    }

    @Test
    fun cloudHasCloudIcon() {
        assertEquals(Icons.Filled.Cloud, NetworkProtocol.CLOUD.icon)
    }

    @Test
    fun allProtocolsHaveUniquePorts() {
        val ports = NetworkProtocol.entries.map { it.defaultPort }
        assertEquals(ports.size, ports.toSet().size)
    }

    @Test
    fun allProtocolsHaveNonZeroPorts() {
        NetworkProtocol.entries.forEach { protocol ->
            assertTrue("${protocol.name} port should be > 0", protocol.defaultPort > 0)
        }
    }

    @Test
    fun valueOfReturnsCorrectProtocol() {
        assertEquals(NetworkProtocol.SMB, NetworkProtocol.valueOf("SMB"))
        assertEquals(NetworkProtocol.FTP, NetworkProtocol.valueOf("FTP"))
        assertEquals(NetworkProtocol.CLOUD, NetworkProtocol.valueOf("CLOUD"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun valueOfThrowsForInvalidName() {
        NetworkProtocol.valueOf("INVALID")
    }
}
