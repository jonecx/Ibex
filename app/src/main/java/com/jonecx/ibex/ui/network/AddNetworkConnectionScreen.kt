package com.jonecx.ibex.ui.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.NetworkConnection
import com.jonecx.ibex.data.model.NetworkProtocol
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNetworkConnectionScreen(
    onNavigateBack: () -> Unit,
    onSave: (NetworkConnection) -> Unit,
    modifier: Modifier = Modifier,
    defaultProtocol: NetworkProtocol = NetworkProtocol.SMB,
    connectionToEdit: NetworkConnection? = null,
) {
    val isEditMode = connectionToEdit != null
    val useIpOctets = remember(connectionToEdit) {
        connectionToEdit?.protocol?.usesIpOctets ?: defaultProtocol.usesIpOctets
    }
    val initialOctets = if (useIpOctets) {
        connectionToEdit?.host?.split(".")?.takeIf { it.size == 4 }
    } else {
        null
    }

    var selectedProtocol by remember { mutableStateOf(connectionToEdit?.protocol ?: defaultProtocol) }
    var protocolExpanded by remember { mutableStateOf(false) }
    var octet1 by remember { mutableStateOf(initialOctets?.getOrNull(0) ?: "") }
    var octet2 by remember { mutableStateOf(initialOctets?.getOrNull(1) ?: "") }
    var octet3 by remember { mutableStateOf(initialOctets?.getOrNull(2) ?: "") }
    var octet4 by remember { mutableStateOf(initialOctets?.getOrNull(3) ?: "") }
    var urlHost by remember { mutableStateOf(if (!useIpOctets) connectionToEdit?.host ?: "" else "") }
    var port by remember { mutableStateOf(connectionToEdit?.port?.toString() ?: selectedProtocol.defaultPort.toString()) }
    var username by remember { mutableStateOf(connectionToEdit?.username ?: "") }
    var password by remember { mutableStateOf(connectionToEdit?.password ?: "") }
    var anonymous by remember { mutableStateOf(connectionToEdit?.anonymous ?: false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf(connectionToEdit?.displayName ?: "") }

    val (focusOctet1, focusOctet2, focusOctet3, focusOctet4) = remember { List(4) { FocusRequester() } }
    val focusPort = remember { FocusRequester() }

    val host by remember {
        derivedStateOf {
            if (selectedProtocol.usesIpOctets) "$octet1.$octet2.$octet3.$octet4" else urlHost.trim()
        }
    }
    val isValid by remember {
        derivedStateOf {
            val hostValid = if (selectedProtocol.usesIpOctets) {
                octet1.isNotBlank() && octet2.isNotBlank() &&
                    octet3.isNotBlank() && octet4.isNotBlank()
            } else {
                urlHost.isNotBlank()
            }
            hostValid && (anonymous || username.isNotBlank())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (isEditMode) R.string.edit_connection_title else R.string.add_connection_title),
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
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = protocolExpanded,
                onExpandedChange = { protocolExpanded = it },
            ) {
                OutlinedTextField(
                    value = stringResource(selectedProtocol.labelRes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.add_connection_protocol)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = protocolExpanded,
                    onDismissRequest = { protocolExpanded = false },
                ) {
                    NetworkProtocol.entries.forEach { protocol ->
                        DropdownMenuItem(
                            text = { Text(stringResource(protocol.labelRes)) },
                            onClick = {
                                selectedProtocol = protocol
                                port = protocol.defaultPort.toString()
                                protocolExpanded = false
                            },
                        )
                    }
                }
            }

            if (selectedProtocol.usesIpOctets) {
                Column {
                    Text(
                        text = stringResource(R.string.add_connection_host),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IpOctetField(value = octet1, onValueChange = { octet1 = it }, focusRequester = focusOctet1, nextFocusRequester = focusOctet2, modifier = Modifier.weight(1f))
                        DotSeparator()
                        IpOctetField(value = octet2, onValueChange = { octet2 = it }, focusRequester = focusOctet2, nextFocusRequester = focusOctet3, modifier = Modifier.weight(1f))
                        DotSeparator()
                        IpOctetField(value = octet3, onValueChange = { octet3 = it }, focusRequester = focusOctet3, nextFocusRequester = focusOctet4, modifier = Modifier.weight(1f))
                        DotSeparator()
                        IpOctetField(value = octet4, onValueChange = { octet4 = it }, focusRequester = focusOctet4, nextFocusRequester = focusPort, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                OutlinedTextField(
                    value = urlHost,
                    onValueChange = { urlHost = it },
                    label = { Text(stringResource(R.string.add_connection_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text(stringResource(R.string.add_connection_port)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusPort),
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.add_connection_username)) },
                singleLine = true,
                enabled = !anonymous,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.add_connection_password)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) {
                                    R.string.add_connection_hide_password
                                } else {
                                    R.string.add_connection_show_password
                                },
                            ),
                        )
                    }
                },
                enabled = !anonymous,
                modifier = Modifier.fillMaxWidth(),
            )

            if (selectedProtocol.supportsAnonymous) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = anonymous,
                        onCheckedChange = { anonymous = it },
                    )
                    Text(
                        text = stringResource(R.string.add_connection_anonymous),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.add_connection_display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val effectiveDisplayName = displayName.trim().ifBlank { host }
                        onSave(
                            NetworkConnection(
                                id = connectionToEdit?.id ?: UUID.randomUUID().toString(),
                                protocol = selectedProtocol,
                                displayName = effectiveDisplayName,
                                host = host,
                                port = port.toIntOrNull() ?: selectedProtocol.defaultPort,
                                username = if (anonymous) "" else username.trim(),
                                password = if (anonymous) "" else password,
                                anonymous = anonymous,
                            ),
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.add_connection_save))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IpOctetField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    nextFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() }.take(3)
            val num = filtered.toIntOrNull()
            if (num == null || num <= 255) {
                onValueChange(filtered)
                if (filtered.length == 3 && nextFocusRequester != null) {
                    nextFocusRequester.requestFocus()
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Composable
private fun DotSeparator() {
    Text(
        text = ".",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.width(8.dp),
    )
}
