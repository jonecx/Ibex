package com.jonecx.ibex.ui.home

import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileSource
import com.jonecx.ibex.data.model.FileSourceType
import com.jonecx.ibex.data.model.FileSources
import com.jonecx.ibex.data.model.SourceStats
import com.jonecx.ibex.data.model.StorageUsage
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.components.SourceTile
import com.jonecx.ibex.util.formatSizeWithCount
import com.jonecx.ibex.util.formatSizeWithCountSpoken
import com.jonecx.ibex.util.formatStorageUsage
import com.jonecx.ibex.util.formatStorageUsageSpoken
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onSourceSelected: (FileSource) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val storageLabel = stringResource(R.string.source_storage)
    val downloadsLabel = stringResource(R.string.source_downloads)
    val imagesLabel = stringResource(R.string.source_images)
    val videosLabel = stringResource(R.string.source_videos)
    val audioLabel = stringResource(R.string.source_audio)
    val documentsLabel = stringResource(R.string.source_documents)
    val appsLabel = stringResource(R.string.source_apps)
    val recentLabel = stringResource(R.string.source_recent)
    val analysisLabel = stringResource(R.string.source_analysis)
    val trashLabel = stringResource(R.string.source_trash)
    val cloudLabel = stringResource(R.string.source_cloud)
    val smbLabel = stringResource(R.string.source_smb)
    val ftpLabel = stringResource(R.string.source_ftp)
    val liveLabel = stringResource(R.string.source_live)

    val localSources = remember(storageLabel) {
        FileSources.getLocalSources(
            storage = storageLabel,
            downloads = downloadsLabel,
            images = imagesLabel,
            videos = videosLabel,
            audio = audioLabel,
            documents = documentsLabel,
            apps = appsLabel,
            recent = recentLabel,
            analysis = analysisLabel,
            trash = trashLabel,
            storageRootPath = Environment.getExternalStorageDirectory().absolutePath,
            downloadsRootPath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
        )
    }
    val remoteSources = remember(cloudLabel) {
        FileSources.getRemoteSources(cloudLabel, smbLabel, ftpLabel, liveLabel)
    }

    HomeScreenContent(
        localSources = localSources,
        remoteSources = remoteSources,
        stats = uiState.stats,
        storageUsage = uiState.storageUsage,
        onSourceSelected = onSourceSelected,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenContent(
    localSources: List<FileSource>,
    remoteSources: List<FileSource>,
    stats: Map<FileSourceType, SourceStats>,
    storageUsage: StorageUsage?,
    onSourceSelected: (FileSource) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localSectionLabel = stringResource(R.string.section_local)
    val remoteSectionLabel = stringResource(R.string.section_remote)

    Scaffold(
        topBar = {
            IbexTopAppBar(
                title = stringResource(R.string.app_name),
                onNavigateBack = {},
                showBackButton = false,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyVerticalGrid(
            // Adaptive so columns grow with width; min wide enough that "size (count)" never truncates.
            columns = GridCells.Adaptive(minSize = 128.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Local sources section
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = localSectionLabel)
            }

            items(localSources) { source ->
                SourceTile(
                    source = source.withStats(stats, storageUsage),
                    onClick = { onSourceSelected(source) },
                )
            }

            // Remote sources section
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = remoteSectionLabel)
            }

            items(remoteSources) { source ->
                SourceTile(
                    source = source.withStats(stats, storageUsage),
                    onClick = { onSourceSelected(source) },
                )
            }
        }
    }
}

// Attaches a subtitle plus a spoken TalkBack label: used/total for Storage, "size (count)" otherwise.
private fun FileSource.withStats(
    stats: Map<FileSourceType, SourceStats>,
    storageUsage: StorageUsage?,
): FileSource {
    if (type == FileSourceType.LOCAL_STORAGE) {
        val usage = storageUsage ?: return this
        return copy(
            subtitle = formatStorageUsage(usage.usedBytes, usage.totalBytes),
            contentDescription = "$name, ${formatStorageUsageSpoken(usage.usedBytes, usage.totalBytes)}",
        )
    }
    val stat = stats[type]?.takeIf { it.count > 0 } ?: return this
    return copy(
        subtitle = formatSizeWithCount(stat.sizeBytes, stat.count),
        contentDescription = "$name, ${formatSizeWithCountSpoken(stat.sizeBytes, stat.count)}",
    )
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Marks the section as a heading so TalkBack users can jump between Local and Remote.
        modifier = modifier
            .padding(vertical = 8.dp)
            .semantics { heading() },
    )
}
