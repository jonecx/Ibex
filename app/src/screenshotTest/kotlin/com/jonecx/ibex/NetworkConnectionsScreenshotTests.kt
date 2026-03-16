package com.jonecx.ibex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.anonymousConnection
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.cloudConnection
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.ftpConnection
import com.jonecx.ibex.fixtures.NetworkConnectionFixtures.smbConnection
import com.jonecx.ibex.ui.network.AddNetworkConnectionScreen
import com.jonecx.ibex.ui.network.NetworkConnectionItem
import com.jonecx.ibex.ui.network.ProtocolSectionHeader
import com.jonecx.ibex.ui.theme.IbexTheme

@Composable
private fun ConnectionItemPreview(connection: NetworkConnection) {
    NetworkConnectionItem(
        connection = connection,
        onClick = {},
        onEditClick = {},
        onDeleteClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun NetworkConnectionItemSmbPreview() {
    IbexTheme {
        ConnectionItemPreview(smbConnection)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun NetworkConnectionItemFtpPreview() {
    IbexTheme {
        ConnectionItemPreview(ftpConnection)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun NetworkConnectionItemCloudPreview() {
    IbexTheme {
        ConnectionItemPreview(cloudConnection)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun NetworkConnectionItemAnonymousPreview() {
    IbexTheme {
        ConnectionItemPreview(anonymousConnection)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun NetworkConnectionsGroupedListPreview() {
    IbexTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            mapOf(
                NetworkProtocol.SMB to listOf(smbConnection, anonymousConnection),
                NetworkProtocol.FTP to listOf(ftpConnection),
                NetworkProtocol.CLOUD to listOf(cloudConnection),
            ).forEach { (protocol, connections) ->
                ProtocolSectionHeader(protocol = protocol)
                connections.forEach { connection ->
                    ConnectionItemPreview(connection)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddNetworkConnectionScreenSmbPreview() {
    IbexTheme {
        AddNetworkConnectionScreen(
            onNavigateBack = {},
            onSave = {},
            defaultProtocol = NetworkProtocol.SMB,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddNetworkConnectionScreenCloudPreview() {
    IbexTheme {
        AddNetworkConnectionScreen(
            onNavigateBack = {},
            onSave = {},
            defaultProtocol = NetworkProtocol.CLOUD,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddNetworkConnectionScreenEditPreview() {
    IbexTheme {
        AddNetworkConnectionScreen(
            onNavigateBack = {},
            onSave = {},
            connectionToEdit = smbConnection,
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NetworkConnectionItemDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        ConnectionItemPreview(smbConnection)
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddNetworkConnectionScreenDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        AddNetworkConnectionScreen(
            onNavigateBack = {},
            onSave = {},
            defaultProtocol = NetworkProtocol.SMB,
        )
    }
}
