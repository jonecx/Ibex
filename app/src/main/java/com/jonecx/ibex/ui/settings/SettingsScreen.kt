package com.jonecx.ibex.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.ui.settings.components.SettingsRadioGroupItem
import com.jonecx.ibex.ui.settings.components.SettingsToggleItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAnalyticsToggleChanged = viewModel::setSendAnalyticsEnabled,
        onViewModeChanged = viewModel::setViewMode,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onAnalyticsToggleChanged: (Boolean) -> Unit,
    onViewModeChanged: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
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
                .padding(paddingValues),
        ) {
            SettingsRadioGroupItem(
                title = stringResource(R.string.settings_view_mode),
                options = ViewMode.entries,
                selectedOption = uiState.viewMode,
                labelFor = { mode ->
                    when (mode) {
                        ViewMode.LIST -> stringResource(R.string.settings_view_mode_list)
                        ViewMode.GRID -> stringResource(R.string.settings_view_mode_grid)
                    }
                },
                onOptionSelected = onViewModeChanged,
            )

            SettingsToggleItem(
                title = stringResource(R.string.settings_send_analytics),
                description = stringResource(R.string.settings_send_analytics_description),
                checked = uiState.sendAnalyticsEnabled,
                onCheckedChange = onAnalyticsToggleChanged,
            )
        }
    }
}
