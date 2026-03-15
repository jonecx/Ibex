package com.jonecx.ibex.data.model

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lan
import androidx.compose.ui.graphics.vector.ImageVector
import com.jonecx.ibex.R
import kotlinx.serialization.Serializable

@Serializable
enum class NetworkProtocol(
    val defaultPort: Int,
    @StringRes val titleRes: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val usesIpOctets: Boolean,
    val supportsAnonymous: Boolean,
) {
    SMB(
        defaultPort = 445,
        titleRes = R.string.network_connections_title_smb,
        labelRes = R.string.add_connection_protocol_smb,
        icon = Icons.Filled.Lan,
        usesIpOctets = true,
        supportsAnonymous = true,
    ),
    FTP(
        defaultPort = 21,
        titleRes = R.string.network_connections_title_ftp,
        labelRes = R.string.add_connection_protocol_ftp,
        icon = Icons.Filled.Folder,
        usesIpOctets = true,
        supportsAnonymous = true,
    ),
    CLOUD(
        defaultPort = 443,
        titleRes = R.string.network_connections_title_cloud,
        labelRes = R.string.add_connection_protocol_cloud,
        icon = Icons.Filled.Cloud,
        usesIpOctets = false,
        supportsAnonymous = false,
    ),
}
