package com.jonecx.ibex.ui.home

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.model.FileSource
import com.jonecx.ibex.data.model.FileSources
import com.jonecx.ibex.ui.components.SourceTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSourceSelected: (FileSource) -> Unit,
    modifier: Modifier = Modifier,
) {
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
    val comingSoonLabel = stringResource(R.string.coming_soon)
    val localSectionLabel = stringResource(R.string.section_local)
    val remoteSectionLabel = stringResource(R.string.section_remote)

    val localSources = remember(storageLabel) {
        FileSources.getLocalSources(
            storageLabel, downloadsLabel, imagesLabel, videosLabel,
            audioLabel, documentsLabel, appsLabel, recentLabel, analysisLabel, trashLabel,
        )
    }
    val remoteSources = remember(cloudLabel) {
        FileSources.getRemoteSources(cloudLabel, smbLabel, ftpLabel, comingSoonLabel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
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
                    source = source,
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
                    source = source,
                    onClick = { onSourceSelected(source) },
                )
            }
        }
    }
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
        modifier = modifier.padding(vertical = 8.dp),
    )
}
