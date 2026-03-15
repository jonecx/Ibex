package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.preferences.NetworkConnectionsPreferencesContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeNetworkConnectionsPreferences : NetworkConnectionsPreferencesContract {
    private val _connections = MutableStateFlow<List<NetworkConnection>>(emptyList())
    override val connections: Flow<List<NetworkConnection>> = _connections

    override suspend fun addConnection(connection: NetworkConnection) {
        _connections.update { it + connection }
    }

    override suspend fun removeConnection(id: String) {
        _connections.update { list -> list.filter { it.id != id } }
    }

    override suspend fun updateConnection(connection: NetworkConnection) {
        _connections.update { list ->
            list.map { if (it.id == connection.id) connection else it }
        }
    }

    fun currentConnections(): List<NetworkConnection> = _connections.value

    fun reset() {
        _connections.value = emptyList()
    }
}
