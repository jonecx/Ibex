package com.jonecx.ibex.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.ThemeMode
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.settings.components.SettingsChoiceRow
import com.jonecx.ibex.ui.settings.components.SettingsNavigationItem
import com.jonecx.ibex.ui.settings.components.SettingsSectionHeader
import com.jonecx.ibex.ui.settings.components.SettingsSwitchRow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayerSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onThemeModeChanged = viewModel::setThemeMode,
        onAnalyticsToggleChanged = viewModel::setSendAnalyticsEnabled,
        onNetworkItemCountToggleChanged = viewModel::setNetworkFolderItemCountEnabled,
        onViewModeChanged = viewModel::setViewMode,
        onGridColumnsChanged = viewModel::setGridColumns,
        onNavigateToPlayerSettings = onNavigateToPlayerSettings,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAnalyticsToggleChanged: (Boolean) -> Unit,
    onNetworkItemCountToggleChanged: (Boolean) -> Unit,
    onViewModeChanged: (ViewMode) -> Unit,
    onGridColumnsChanged: (Int) -> Unit,
    onNavigateToPlayerSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            IbexTopAppBar(
                title = stringResource(R.string.settings),
                onNavigateBack = onNavigateBack,
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_theme),
                    options = listOf(
                        stringResource(R.string.settings_theme_system) to ThemeMode.SYSTEM,
                        stringResource(R.string.settings_theme_light) to ThemeMode.LIGHT,
                        stringResource(R.string.settings_theme_dark) to ThemeMode.DARK,
                    ),
                    selected = uiState.themeMode,
                    onSelect = onThemeModeChanged,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(stringResource(R.string.settings_section_display))
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_view_mode),
                    options = listOf(
                        stringResource(R.string.settings_view_mode_list) to ViewMode.LIST,
                        stringResource(R.string.settings_view_mode_grid) to ViewMode.GRID,
                    ),
                    selected = uiState.viewMode,
                    onSelect = onViewModeChanged,
                )
                if (uiState.viewMode == ViewMode.GRID) {
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_grid_columns),
                        options = SettingsPreferencesContract.GRID_COLUMN_OPTIONS.map { it.toString() to it },
                        selected = uiState.gridColumns,
                        onSelect = onGridColumnsChanged,
                    )
                }
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_network_item_count),
                    hint = stringResource(R.string.settings_network_item_count_description),
                    checked = uiState.networkFolderItemCountEnabled,
                    onChange = onNetworkItemCountToggleChanged,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSectionHeader(stringResource(R.string.settings_section_privacy))
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_send_analytics),
                    hint = stringResource(R.string.settings_send_analytics_description),
                    checked = uiState.sendAnalyticsEnabled,
                    onChange = onAnalyticsToggleChanged,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsNavigationItem(
                title = stringResource(R.string.settings_player),
                description = stringResource(R.string.settings_player_description),
                onClick = onNavigateToPlayerSettings,
            )
        }
    }
}
