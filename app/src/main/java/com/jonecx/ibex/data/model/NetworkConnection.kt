package com.jonecx.ibex.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.util.UUID

@Immutable
@Serializable
data class NetworkConnection(
    val id: String = UUID.randomUUID().toString(),
    val protocol: NetworkProtocol = NetworkProtocol.SMB,
    val displayName: String,
    val host: String,
    val port: Int = protocol.defaultPort,
    val username: String = "",
    val password: String = "",
    val anonymous: Boolean = false,
)
