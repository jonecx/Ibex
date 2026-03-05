package com.jonecx.ibex.ui.analysis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.StorageBreakdown
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_APPS
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_AUDIO
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_DOCUMENTS
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_IMAGES
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_OTHER
import com.jonecx.ibex.data.repository.StorageAnalyzer.Companion.CATEGORY_VIDEOS
import com.jonecx.ibex.ui.components.ErrorView
import com.jonecx.ibex.ui.components.LoadingView
import com.jonecx.ibex.ui.components.PieChart
import com.jonecx.ibex.ui.components.PieChartSegment
import com.jonecx.ibex.ui.theme.GrayDark
import com.jonecx.ibex.util.formatFileSize

@Composable
fun StorageAnalysisScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageAnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    StorageAnalysisScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::analyze,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StorageAnalysisScreenContent(
    uiState: StorageAnalysisUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.analysis_title),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> LoadingView()
                uiState.error != null -> ErrorView(
                    message = uiState.error?.message ?: stringResource(R.string.unknown_error),
                    onRetry = onRetry,
                )
                uiState.breakdown != null -> StorageAnalysisContent(breakdown = uiState.breakdown!!)
            }
        }
    }
}

@Composable
private fun StorageAnalysisContent(
    breakdown: StorageBreakdown,
    modifier: Modifier = Modifier,
) {
    val freeBytes = breakdown.totalBytes - breakdown.usedBytes
    val categoryLabels = mapOf(
        CATEGORY_IMAGES to stringResource(R.string.analysis_category_images),
        CATEGORY_VIDEOS to stringResource(R.string.analysis_category_videos),
        CATEGORY_AUDIO to stringResource(R.string.analysis_category_audio),
        CATEGORY_DOCUMENTS to stringResource(R.string.analysis_category_documents),
        CATEGORY_APPS to stringResource(R.string.analysis_category_apps),
        CATEGORY_OTHER to stringResource(R.string.analysis_category_other),
    )

    val segments = breakdown.categories
        .filter { it.sizeBytes > 0 }
        .map { category ->
            PieChartSegment(
                label = "${categoryLabels[category.name] ?: category.name} (${formatFileSize(category.sizeBytes)})",
                value = category.sizeBytes.toFloat(),
                color = category.color,
            )
        } + if (freeBytes > 0) {
        listOf(
            PieChartSegment(
                label = "${stringResource(R.string.analysis_category_free)} (${formatFileSize(freeBytes)})",
                value = freeBytes.toFloat(),
                color = GrayDark,
            ),
        )
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StorageSummary(breakdown = breakdown, freeBytes = freeBytes)

        Spacer(modifier = Modifier.height(24.dp))

        PieChart(
            segments = segments,
            chartSize = 220.dp,
            strokeWidth = 36.dp,
        )
    }
}

@Composable
private fun StorageSummary(
    breakdown: StorageBreakdown,
    freeBytes: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatFileSize(breakdown.totalBytes),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.analysis_total_storage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Text(
                text = "${stringResource(R.string.analysis_used)}: ${formatFileSize(breakdown.usedBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "  •  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${stringResource(R.string.analysis_free)}: ${formatFileSize(freeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
