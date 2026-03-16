package com.jonecx.ibex.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkConnectionTest {

    private val defaultConnection = NetworkConnection(displayName = "Test", host = "192.168.1.1")

    @Test
    fun defaultProtocolIsSMB() {
        assertEquals(NetworkProtocol.SMB, defaultConnection.protocol)
    }

    @Test
    fun defaultPortMatchesProtocol() {
        val smb = NetworkConnection(displayName = "SMB", host = "192.168.1.1")
        assertEquals(445, smb.port)

        val ftp = NetworkConnection(protocol = NetworkProtocol.FTP, displayName = "FTP", host = "192.168.1.1")
        assertEquals(21, ftp.port)

        val cloud = NetworkConnection(protocol = NetworkProtocol.CLOUD, displayName = "Cloud", host = "cloud.example.com")
        assertEquals(443, cloud.port)
    }

    @Test
    fun defaultUsernameIsEmpty() {
        assertEquals("", defaultConnection.username)
    }

    @Test
    fun defaultPasswordIsEmpty() {
        assertEquals("", defaultConnection.password)
    }

    @Test
    fun defaultAnonymousIsFalse() {
        assertFalse(defaultConnection.anonymous)
    }

    @Test
    fun generatesUniqueIds() {
        val a = NetworkConnection(displayName = "A", host = "192.168.1.1")
        val b = NetworkConnection(displayName = "B", host = "192.168.1.2")
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun customPortOverridesDefault() {
        val connection = NetworkConnection(displayName = "Test", host = "192.168.1.1", port = 9999)
        assertEquals(9999, connection.port)
    }

    @Test
    fun serializationRoundTrip() {
        val original = NetworkConnection(
            id = "test-id",
            protocol = NetworkProtocol.FTP,
            displayName = "My FTP",
            host = "192.168.1.100",
            port = 2121,
            username = "admin",
            password = "secret",
            anonymous = false,
        )
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<NetworkConnection>(json)
        assertEquals(original, deserialized)
    }

    @Test
    fun serializationRoundTripAnonymous() {
        val original = NetworkConnection(
            id = "anon-id",
            protocol = NetworkProtocol.SMB,
            displayName = "Anon SMB",
            host = "10.0.0.1",
            anonymous = true,
        )
        val json = Json.encodeToString(original)
        val deserialized = Json.decodeFromString<NetworkConnection>(json)
        assertEquals(original, deserialized)
        assertTrue(deserialized.anonymous)
    }

    @Test
    fun listSerializationRoundTrip() {
        val connections = listOf(
            NetworkConnection(id = "1", displayName = "SMB", host = "10.0.0.1"),
            NetworkConnection(id = "2", protocol = NetworkProtocol.FTP, displayName = "FTP", host = "10.0.0.2"),
            NetworkConnection(id = "3", protocol = NetworkProtocol.CLOUD, displayName = "Cloud", host = "cloud.example.com"),
        )
        val json = Json.encodeToString(connections)
        val deserialized = Json.decodeFromString<List<NetworkConnection>>(json)
        assertEquals(connections, deserialized)
    }

    @Test
    fun copyPreservesAllFields() {
        val original = NetworkConnection(
            id = "copy-id",
            protocol = NetworkProtocol.CLOUD,
            displayName = "Cloud Drive",
            host = "drive.example.com",
            port = 8443,
            username = "user",
            password = "pass",
            anonymous = false,
        )
        val copy = original.copy(displayName = "Renamed")
        assertEquals("Renamed", copy.displayName)
        assertEquals(original.id, copy.id)
        assertEquals(original.protocol, copy.protocol)
        assertEquals(original.host, copy.host)
        assertEquals(original.port, copy.port)
        assertEquals(original.username, copy.username)
        assertEquals(original.password, copy.password)
        assertEquals(original.anonymous, copy.anonymous)
    }
}
