package com.jonecx.ibex.ui.network

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.analytics.AnalyticsManager
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.util.launchCollect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class NetworkConnectionsUiState(
    val connections: List<NetworkConnection> = emptyList(),
    val connectionToEdit: NetworkConnection? = null,
    val defaultProtocol: NetworkProtocol = NetworkProtocol.SMB,
)

class NetworkConnectionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val networkPreferences: NetworkConnectionsPreferencesContract,
    private val analyticsManager: AnalyticsManager,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val defaultProtocol: NetworkProtocol = savedStateHandle.get<String>(ARG_PROTOCOL)
        ?.let { runCatching { NetworkProtocol.valueOf(it) }.getOrNull() }
        ?: NetworkProtocol.SMB

    private val _uiState = MutableStateFlow(NetworkConnectionsUiState(defaultProtocol = defaultProtocol))
    val uiState: StateFlow<NetworkConnectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launchCollect(networkPreferences.connections, dispatcher) { connections ->
            _uiState.update { it.copy(connections = connections) }
        }
    }

    fun addConnection(connection: NetworkConnection) {
        analyticsManager.trackConnectionAdded(connection.protocol, connection.anonymous)
        viewModelScope.launch(dispatcher) {
            networkPreferences.addConnection(connection)
        }
    }

    fun updateConnection(connection: NetworkConnection) {
        analyticsManager.trackConnectionEdited(connection.protocol, connection.anonymous)
        viewModelScope.launch(dispatcher) {
            networkPreferences.updateConnection(connection)
        }
    }

    fun removeConnection(id: String) {
        // Resolve the protocol from state before the row is gone; credentials/host are never logged.
        val protocol = _uiState.value.connections.firstOrNull { it.id == id }?.protocol
        analyticsManager.trackConnectionDeleted(protocol)
        viewModelScope.launch(dispatcher) {
            networkPreferences.removeConnection(id)
        }
    }

    fun setConnectionToEdit(connection: NetworkConnection) {
        _uiState.update { it.copy(connectionToEdit = connection) }
    }

    fun clearConnectionToEdit() {
        _uiState.update { it.copy(connectionToEdit = null) }
    }

    companion object {
        const val ARG_PROTOCOL = "protocol"
    }
}
