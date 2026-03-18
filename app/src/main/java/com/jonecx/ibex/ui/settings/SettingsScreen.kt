package com.jonecx.ibex.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.ViewMode
import com.jonecx.ibex.data.preferences.SettingsPreferencesContract
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.settings.components.SettingsRadioGroupItem
import com.jonecx.ibex.ui.settings.components.SettingsStepSliderItem
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
        onGridColumnsChanged = viewModel::setGridColumns,
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
    onGridColumnsChanged: (Int) -> Unit,
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
                optionExtra = { mode ->
                    if (mode == ViewMode.GRID) {
                        SettingsStepSliderItem(
                            title = stringResource(R.string.settings_grid_columns),
                            value = uiState.gridColumns,
                            steps = SettingsPreferencesContract.GRID_COLUMN_OPTIONS,
                            onValueChanged = onGridColumnsChanged,
                        )
                    }
                },
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
