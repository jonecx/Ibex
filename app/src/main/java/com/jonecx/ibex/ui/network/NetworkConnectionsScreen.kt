package com.jonecx.ibex.ui.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import com.jonecx.ibex.ui.components.ConfirmationDialog
import com.jonecx.ibex.ui.components.EmptyView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkConnectionsScreen(
    onNavigateBack: () -> Unit,
    onConnectionSelected: (NetworkConnection) -> Unit,
    onAddConnection: () -> Unit,
    onEditConnection: (NetworkConnection) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NetworkConnectionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var connectionToDelete by remember { mutableStateOf<NetworkConnection?>(null) }

    val groupedConnections by remember {
        derivedStateOf { uiState.connections.groupBy { it.protocol } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.network_connections_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_up),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddConnection,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.network_add_connection),
                )
            }
        },
        modifier = modifier,
    ) { paddingValues ->
        if (uiState.connections.isEmpty()) {
            EmptyView(
                modifier = Modifier.padding(paddingValues),
                message = stringResource(R.string.network_no_connections),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                groupedConnections.forEach { (protocol, connections) ->
                    item(key = "header_${protocol.name}") {
                        ProtocolSectionHeader(protocol = protocol)
                    }
                    items(connections, key = { it.id }) { connection ->
                        NetworkConnectionItem(
                            connection = connection,
                            onClick = { onConnectionSelected(connection) },
                            onEditClick = { onEditConnection(connection) },
                            onDeleteClick = { connectionToDelete = connection },
                        )
                    }
                }
            }
        }
    }

    connectionToDelete?.let { connection ->
        ConfirmationDialog(
            title = stringResource(R.string.network_delete_title),
            message = stringResource(R.string.network_delete_message, connection.displayName),
            confirmText = stringResource(R.string.network_delete_confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.removeConnection(connection.id)
                connectionToDelete = null
            },
            onDismiss = { connectionToDelete = null },
        )
    }
}

@Composable
private fun ProtocolSectionHeader(
    protocol: NetworkProtocol,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = protocol.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(protocol.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NetworkConnectionItem(
    connection: NetworkConnection,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = connection.protocol.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = connection.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!connection.anonymous && connection.username.isNotBlank()) {
                    Text(
                        text = connection.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.network_edit_connection),
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.network_delete_connection),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
