package com.jonecx.ibex.data.preferences

import com.jonecx.ibex.data.model.NetworkConnection
import kotlinx.coroutines.flow.Flow

interface NetworkConnectionsPreferencesContract {
    val connections: Flow<List<NetworkConnection>>
    suspend fun addConnection(connection: NetworkConnection)
    suspend fun removeConnection(id: String)
    suspend fun updateConnection(connection: NetworkConnection)
}
