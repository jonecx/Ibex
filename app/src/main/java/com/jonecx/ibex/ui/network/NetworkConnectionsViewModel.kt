package com.jonecx.ibex.ui.network

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import com.jonecx.ibex.di.IoDispatcher
import com.jonecx.ibex.util.launchCollect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class NetworkConnectionsUiState(
    val connections: List<NetworkConnection> = emptyList(),
    val connectionToEdit: NetworkConnection? = null,
    val defaultProtocol: NetworkProtocol = NetworkProtocol.SMB,
)

@HiltViewModel
class NetworkConnectionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val networkPreferences: NetworkConnectionsPreferencesContract,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
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
        viewModelScope.launch(dispatcher) {
            networkPreferences.addConnection(connection)
        }
    }

    fun updateConnection(connection: NetworkConnection) {
        viewModelScope.launch(dispatcher) {
            networkPreferences.updateConnection(connection)
        }
    }

    fun removeConnection(id: String) {
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
