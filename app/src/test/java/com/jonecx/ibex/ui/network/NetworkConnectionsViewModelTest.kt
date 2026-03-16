package com.jonecx.ibex.ui.network

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.fixtures.FakeNetworkConnectionsPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConnectionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePreferences: FakeNetworkConnectionsPreferences

    @Before
    fun setup() {
        fakePreferences = FakeNetworkConnectionsPreferences()
    }

    private fun createViewModel(
        protocol: String? = null,
    ): NetworkConnectionsViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (protocol != null) set(NetworkConnectionsViewModel.ARG_PROTOCOL, protocol)
        }
        return NetworkConnectionsViewModel(savedStateHandle, fakePreferences, testDispatcher)
    }

    @Test
    fun initialStateHasEmptyConnections() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertTrue(awaitItem().connections.isEmpty())
        }
    }

    @Test
    fun defaultProtocolIsSmbWhenNoArgProvided() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertEquals(NetworkProtocol.SMB, awaitItem().defaultProtocol)
        }
    }

    @Test
    fun defaultProtocolIsFtpWhenFtpArgProvided() = runTest {
        val viewModel = createViewModel(protocol = "FTP")
        viewModel.uiState.test {
            assertEquals(NetworkProtocol.FTP, awaitItem().defaultProtocol)
        }
    }

    @Test
    fun defaultProtocolIsCloudWhenCloudArgProvided() = runTest {
        val viewModel = createViewModel(protocol = "CLOUD")
        viewModel.uiState.test {
            assertEquals(NetworkProtocol.CLOUD, awaitItem().defaultProtocol)
        }
    }

    @Test
    fun defaultProtocolFallsBackToSmbForInvalidArg() = runTest {
        val viewModel = createViewModel(protocol = "INVALID")
        viewModel.uiState.test {
            assertEquals(NetworkProtocol.SMB, awaitItem().defaultProtocol)
        }
    }

    @Test
    fun addConnectionUpdatesState() = runTest {
        val viewModel = createViewModel()
        val connection = NetworkConnection(
            id = "test-1",
            displayName = "My SMB",
            host = "192.168.1.1",
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().connections.isEmpty())

            viewModel.addConnection(connection)
            val state = awaitItem()
            assertEquals(1, state.connections.size)
            assertEquals(connection, state.connections.first())
        }
    }

    @Test
    fun addMultipleConnectionsUpdatesState() = runTest {
        val viewModel = createViewModel()
        val smb = NetworkConnection(id = "1", displayName = "SMB", host = "10.0.0.1")
        val ftp = NetworkConnection(id = "2", protocol = NetworkProtocol.FTP, displayName = "FTP", host = "10.0.0.2")

        viewModel.uiState.test {
            assertTrue(awaitItem().connections.isEmpty())

            viewModel.addConnection(smb)
            assertEquals(1, awaitItem().connections.size)

            viewModel.addConnection(ftp)
            val state = awaitItem()
            assertEquals(2, state.connections.size)
            assertEquals(smb, state.connections[0])
            assertEquals(ftp, state.connections[1])
        }
    }

    @Test
    fun removeConnectionUpdatesState() = runTest {
        val viewModel = createViewModel()
        val connection = NetworkConnection(id = "to-remove", displayName = "Remove Me", host = "10.0.0.1")

        viewModel.uiState.test {
            assertTrue(awaitItem().connections.isEmpty())

            viewModel.addConnection(connection)
            assertEquals(1, awaitItem().connections.size)

            viewModel.removeConnection(connection.id)
            assertTrue(awaitItem().connections.isEmpty())
        }
    }

    @Test
    fun removeConnectionOnlyRemovesTarget() = runTest {
        val viewModel = createViewModel()
        val keep = NetworkConnection(id = "keep", displayName = "Keep", host = "10.0.0.1")
        val remove = NetworkConnection(id = "remove", displayName = "Remove", host = "10.0.0.2")

        viewModel.uiState.test {
            assertTrue(awaitItem().connections.isEmpty())

            viewModel.addConnection(keep)
            assertEquals(1, awaitItem().connections.size)

            viewModel.addConnection(remove)
            assertEquals(2, awaitItem().connections.size)

            viewModel.removeConnection(remove.id)
            val state = awaitItem()
            assertEquals(1, state.connections.size)
            assertEquals(keep, state.connections.first())
        }
    }

    @Test
    fun updateConnectionUpdatesState() = runTest {
        val viewModel = createViewModel()
        val original = NetworkConnection(id = "update-me", displayName = "Original", host = "10.0.0.1")
        val updated = original.copy(displayName = "Updated", host = "10.0.0.99")

        viewModel.uiState.test {
            assertTrue(awaitItem().connections.isEmpty())

            viewModel.addConnection(original)
            assertEquals("Original", awaitItem().connections.first().displayName)

            viewModel.updateConnection(updated)
            val state = awaitItem()
            assertEquals(1, state.connections.size)
            assertEquals("Updated", state.connections.first().displayName)
            assertEquals("10.0.0.99", state.connections.first().host)
        }
    }

    @Test
    fun setConnectionToEditUpdatesState() = runTest {
        val viewModel = createViewModel()
        val connection = NetworkConnection(id = "edit-me", displayName = "Edit", host = "10.0.0.1")

        viewModel.uiState.test {
            assertNull(awaitItem().connectionToEdit)

            viewModel.setConnectionToEdit(connection)
            assertEquals(connection, awaitItem().connectionToEdit)
        }
    }

    @Test
    fun clearConnectionToEditResetsState() = runTest {
        val viewModel = createViewModel()
        val connection = NetworkConnection(id = "edit-me", displayName = "Edit", host = "10.0.0.1")

        viewModel.uiState.test {
            assertNull(awaitItem().connectionToEdit)

            viewModel.setConnectionToEdit(connection)
            assertEquals(connection, awaitItem().connectionToEdit)

            viewModel.clearConnectionToEdit()
            assertNull(awaitItem().connectionToEdit)
        }
    }

    @Test
    fun connectionToEditDoesNotAffectConnectionsList() = runTest {
        val viewModel = createViewModel()
        val connection = NetworkConnection(id = "edit-me", displayName = "Edit", host = "10.0.0.1")

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial.connections.isEmpty())

            viewModel.setConnectionToEdit(connection)
            val state = awaitItem()
            assertEquals(connection, state.connectionToEdit)
            assertTrue(state.connections.isEmpty())
        }
    }
}
