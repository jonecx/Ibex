package com.jonecx.ibex.data.repository

import app.cash.turbine.test
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import com.jonecx.ibex.fixtures.FakeSmbContextProvider
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SmbFileRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakePreferences = FakeNetworkConnectionsPreferences()
    private val fakeSmbContextProvider = FakeSmbContextProvider()

    private fun createRepository(connectionId: String = "smb-1"): SmbFileRepository {
        return SmbFileRepository(
            connectionId = connectionId,
            networkPreferences = fakePreferences,
            ioDispatcher = testDispatcher,
            smbContextProvider = fakeSmbContextProvider,
        )
    }

    private suspend fun assertConnectionNotFoundError(flow: Flow<*>) {
        flow.test {
            val error = awaitError()
            assertTrue(error is IllegalStateException)
            assertTrue(error.message!!.contains("Connection not found"))
        }
    }

    private fun assertContextRegistered(connection: NetworkConnection) {
        assertTrue(
            "CIFSContext should be registered for host ${connection.host}",
            fakeSmbContextProvider.get(connection.host) != null,
        )
    }

    @Test
    fun `ensureTrailingSlash adds slash when missing`() {
        assertEquals(
            "smb://host/share/",
            SmbFileRepository.ensureTrailingSlash("smb://host/share"),
        )
    }

    @Test
    fun `ensureTrailingSlash preserves existing slash`() {
        assertEquals(
            "smb://host/share/",
            SmbFileRepository.ensureTrailingSlash("smb://host/share/"),
        )
    }

    @Test
    fun `ensureTrailingSlash works with deep paths`() {
        assertEquals(
            "smb://host/share/folder/subfolder/",
            SmbFileRepository.ensureTrailingSlash("smb://host/share/folder/subfolder"),
        )
    }

    @Test
    fun `getFiles throws when connection not found`() = runTest {
        val repo = createRepository(connectionId = "nonexistent")
        assertConnectionNotFoundError(repo.getFiles("smb://host/share/"))
    }

    @Test
    fun `getStorageRoots throws when connection not found`() = runTest {
        val repo = createRepository(connectionId = "nonexistent")
        assertConnectionNotFoundError(repo.getStorageRoots())
    }

    @Test
    fun `getFileDetails returns null when connection not found`() = runTest {
        val repo = createRepository(connectionId = "nonexistent")
        val result = repo.getFileDetails("smb://host/share/file.txt")
        assertEquals(null, result)
    }

    @Test
    fun `getFiles resolves connection from preferences`() = runTest {
        fakePreferences.addConnection(NetworkConnectionFixtures.smbConnection)
        val repo = createRepository(connectionId = "smb-1")

        repo.getFiles("smb://192.168.1.100/share/").test {
            // Will fail at SmbFile network level, but connection resolution should succeed
            val error = awaitError()
            // The error should NOT be "Connection not found" — it should be a network/SMB error
            assertTrue(
                "Expected network error, got: ${error.message}",
                error !is IllegalStateException || !error.message!!.contains("Connection not found"),
            )
        }
    }

    @Test
    fun `getFiles registers context with smbContextProvider`() = runTest {
        fakePreferences.addConnection(NetworkConnectionFixtures.smbConnection)
        val repo = createRepository(connectionId = "smb-1")

        repo.getFiles("smb://192.168.1.100/share/").test {
            awaitError() // network error expected
        }

        assertContextRegistered(NetworkConnectionFixtures.smbConnection)
    }

    @Test
    fun `anonymous connection creates context without credentials`() = runTest {
        fakePreferences.addConnection(NetworkConnectionFixtures.anonymousConnection)
        val repo = createRepository(connectionId = "anon-1")

        repo.getFiles("smb://192.168.1.50/share/").test {
            awaitError() // network error expected
        }

        assertContextRegistered(NetworkConnectionFixtures.anonymousConnection)
    }
}
