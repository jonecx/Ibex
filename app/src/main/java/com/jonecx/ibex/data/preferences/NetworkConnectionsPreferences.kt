package com.jonecx.ibex.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jonecx.ibex.data.crypto.CryptoManager
import com.jonecx.ibex.data.model.NetworkConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.networkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NetworkConnectionsPreferences.STORE_NAME,
)

@Singleton
class NetworkConnectionsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
) : NetworkConnectionsPreferencesContract {
    private val dataStore = context.networkDataStore

    override val connections: Flow<List<NetworkConnection>> = dataStore.data.map { preferences ->
        preferences.currentConnections().map { it.decryptCredentials() }
    }

    override suspend fun addConnection(connection: NetworkConnection) {
        dataStore.edit { preferences ->
            val updated = preferences.currentConnections() + connection.encryptCredentials()
            preferences[CONNECTIONS_KEY] = toJson(updated)
        }
    }

    override suspend fun removeConnection(id: String) {
        dataStore.edit { preferences ->
            val updated = preferences.currentConnections().filter { it.id != id }
            preferences[CONNECTIONS_KEY] = toJson(updated)
        }
    }

    override suspend fun updateConnection(connection: NetworkConnection) {
        dataStore.edit { preferences ->
            val updated = preferences.currentConnections().map {
                if (it.id == connection.id) connection.encryptCredentials() else it
            }
            preferences[CONNECTIONS_KEY] = toJson(updated)
        }
    }

    private fun NetworkConnection.encryptCredentials(): NetworkConnection = copy(
        username = cryptoManager.encrypt(username),
        password = cryptoManager.encrypt(password),
    )

    private fun NetworkConnection.decryptCredentials(): NetworkConnection = copy(
        username = cryptoManager.decrypt(username),
        password = cryptoManager.decrypt(password),
    )

    private fun Preferences.currentConnections(): List<NetworkConnection> =
        Json.decodeFromString(this[CONNECTIONS_KEY] ?: EMPTY_JSON_ARRAY)

    private fun toJson(connections: List<NetworkConnection>): String =
        Json.encodeToString(connections)

    companion object {
        const val STORE_NAME = "network_connections"
        private val CONNECTIONS_KEY = stringPreferencesKey(STORE_NAME)
        private const val EMPTY_JSON_ARRAY = "[]"
    }
}
