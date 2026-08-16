package com.jonecx.ibex.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.data.transfer.TransferProgress
import com.jonecx.ibex.data.transfer.TransferSnapshot
import com.jonecx.ibex.data.transfer.TransferStatus
import com.jonecx.ibex.ui.explorer.components.fileTypeGlyph
import com.jonecx.ibex.ui.theme.SourceSmbColor
import com.jonecx.ibex.util.FileTypeUtils
import com.jonecx.ibex.util.formatDurationShort
import com.jonecx.ibex.util.formatFileSize
import com.jonecx.ibex.util.formatTransferSpeed
import org.koin.core.context.GlobalContext
import kotlin.math.roundToInt

// The per-job controls the panel exposes, bundled so the stateless content stays easy to preview/test.
// Immutable + remembered once, so passing it to the cards keeps them skippable across progress ticks.
@Immutable
class TransferDetailActions(
    val onPause: (String) -> Unit,
    val onResume: (String) -> Unit,
    val onCancel: (String) -> Unit,
    val onPauseAll: () -> Unit,
    val onRetry: (String) -> Unit,
    val onDismiss: (String) -> Unit,
) {
    companion object {
        val Noop = TransferDetailActions(
            onPause = {},
            onResume = {},
            onCancel = {},
            onPauseAll = {},
            onRetry = {},
            onDismiss = {},
        )
    }
}

// Thin entry point: pulls live transfer state from the app-scoped manager. Renders nothing when idle,
// so it can sit unconditionally under any screen's toolbar with zero cost while no transfer is running.
// Resolves Koin directly (not koinInject) so it also no-ops cleanly in previews/screenshots with no graph.
@Composable
fun TransferProgressBar(modifier: Modifier = Modifier) {
    val manager = remember { GlobalContext.getOrNull()?.get<TransferManager>() } ?: return
    val snapshot by manager.snapshot.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val actions = remember(manager) {
        TransferDetailActions(
            onPause = manager::pause,
            onResume = manager::resume,
            onCancel = manager::cancel,
            onPauseAll = manager::pauseAll,
            onRetry = manager::retry,
            onDismiss = manager::dismiss,
        )
    }

    // Fold the panel back up once the last job clears, so the next transfer starts collapsed.
    if (!snapshot.hasActive && expanded) {
        LaunchedEffect(Unit) { expanded = false }
    }

    TransferProgressBarContent(
        snapshot = snapshot,
        expanded = expanded,
        onToggleExpanded = { expanded = !expanded },
        actions = actions,
        modifier = modifier,
    )
}

// The under-toolbar bar: a thin line, an aggregate row, and the detail panel that unfurls inline on tap.
// Stateless (expansion hoisted) so both states render directly in previews and tests.
@Composable
fun TransferProgressBarContent(
    snapshot: TransferSnapshot,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    actions: TransferDetailActions,
    modifier: Modifier = Modifier,
) {
    if (!snapshot.hasActive) return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            AggregateProgressLine(fraction = snapshot.fraction, indeterminate = snapshot.isCounting)
            AggregateRow(snapshot = snapshot, expanded = expanded, onToggle = onToggleExpanded)
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Cap the height so a long queue scrolls inside the panel instead of pushing the screen down.
                    Column(
                        modifier = Modifier
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        TransferDetailContent(snapshot = snapshot, actions = actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun AggregateProgressLine(fraction: Float, indeterminate: Boolean) {
    if (indeterminate) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    } else {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun AggregateRow(snapshot: TransferSnapshot, expanded: Boolean, onToggle: () -> Unit) {
    val stateLabel = stringResource(if (expanded) R.string.transfer_expanded else R.string.transfer_collapsed)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = stringResource(if (expanded) R.string.transfer_collapse else R.string.transfer_expand),
                role = Role.Button,
                onClick = onToggle,
            )
            .semantics { stateDescription = stateLabel }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Nothing is moving and only failures remain: the row becomes a "tap to retry" error affordance.
        val failedOnly = snapshot.hasFailed && !snapshot.hasRunningOrQueued && !snapshot.hasPaused
        Icon(
            imageVector = if (failedOnly) Icons.Filled.ErrorOutline else snapshot.primaryOperation.icon(),
            contentDescription = null,
            tint = if (failedOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = if (failedOnly) {
                    failedTitle(snapshot.failedCount)
                } else {
                    titleFor(snapshot.primaryOperation, snapshot.totalFiles)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (failedOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (failedOnly) stringResource(R.string.transfer_tap_retry) else collapsedSubtitle(snapshot),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Decorative: the row itself carries the button role, action label, and expanded/collapsed state.
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Stateless panel body: a header plus a card per job. The screenshot/preview target, and the content the
// bar unfurls inline on tap.
@Composable
fun TransferDetailContent(
    snapshot: TransferSnapshot,
    actions: TransferDetailActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
    ) {
        // Only a "Pause all" affordance here; the aggregate row above already labels the section. Kept in
        // its own row (not next to the per-job buttons) so it is nowhere near an accidental Cancel tap.
        if (snapshot.hasRunningOrQueued) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
            ) {
                TextButton(onClick = actions.onPauseAll) {
                    Text(
                        text = stringResource(R.string.transfer_pause_all),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        snapshot.jobs.forEach { job ->
            when (job.status) {
                TransferStatus.RUNNING, TransferStatus.PAUSED ->
                    ActiveJobCard(job = job, actions = actions)
                TransferStatus.QUEUED ->
                    QueuedJobCard(job = job, onCancel = { actions.onCancel(job.id) })
                TransferStatus.FAILED ->
                    FailedJobCard(
                        job = job,
                        onRetry = { actions.onRetry(job.id) },
                        onDismiss = { actions.onDismiss(job.id) },
                    )
                else -> Unit
            }
        }
    }
}

@Composable
private fun ActiveJobCard(job: TransferProgress, actions: TransferDetailActions) {
    JobCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = job.operation.icon(),
                contentDescription = null,
                tint = if (job.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = destinationLine(job.operation, job.destinationDir),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
        }

        LinearProgressIndicator(
            progress = { job.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            color = if (job.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Text(
            text = activeStats(job),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )

        val currentName = job.currentFileName
        if (currentName != null && !job.isPaused) {
            CurrentFileRow(name = currentName, fraction = job.currentFileFraction)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            if (job.isPaused) {
                JobActionButton(
                    icon = Icons.Filled.PlayArrow,
                    label = stringResource(R.string.transfer_resume),
                    onClick = { actions.onResume(job.id) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                JobActionButton(
                    icon = Icons.Filled.Pause,
                    label = stringResource(R.string.transfer_pause),
                    onClick = { actions.onPause(job.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            JobActionButton(
                icon = Icons.Filled.Close,
                label = stringResource(R.string.transfer_cancel_action),
                onClick = { actions.onCancel(job.id) },
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CurrentFileRow(name: String, fraction: Float) {
    val (icon, tint) = fileTypeGlyph(FileTypeUtils.getFileTypeFromName(name))
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                text = stringResource(R.string.transfer_percent, (fraction * 100).roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun QueuedJobCard(job: TransferProgress, onCancel: () -> Unit) {
    JobCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = job.operation.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            ) {
                Text(
                    text = destinationLine(job.operation, job.destinationDir),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = queuedSubtitle(job),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.transfer_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FailedJobCard(job: TransferProgress, onRetry: () -> Unit, onDismiss: () -> Unit) {
    JobCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = destinationLine(job.operation, job.destinationDir),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
        }
        Text(
            text = stringResource(R.string.transfer_failed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            JobActionButton(
                icon = Icons.Filled.Refresh,
                label = stringResource(R.string.transfer_retry),
                onClick = onRetry,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            JobActionButton(
                icon = Icons.Filled.Close,
                label = stringResource(R.string.transfer_dismiss),
                onClick = onDismiss,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun JobCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun JobActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// "Moving to " / "Copying to " then the destination, with an SMB host tinted in the source accent.
@Composable
private fun destinationLine(operation: ClipboardOperation, destinationDir: String) = buildAnnotatedString {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val verb = when (operation) {
        ClipboardOperation.MOVE -> stringResource(R.string.transfer_dest_moving)
        ClipboardOperation.COPY -> stringResource(R.string.transfer_dest_copying)
    }
    withStyle(SpanStyle(color = muted)) { append("$verb ") }

    if (destinationDir.startsWith(FileTypeUtils.SMB_SCHEME_PREFIX)) {
        val host = FileTypeUtils.smbExtractHost(destinationDir).orEmpty()
        withStyle(SpanStyle(color = SourceSmbColor, fontWeight = FontWeight.SemiBold)) { append(host) }
        val rest = destinationDir.removePrefix(FileTypeUtils.SMB_SCHEME_PREFIX)
            .removePrefix(host)
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }
        rest.forEach { segment ->
            withStyle(SpanStyle(color = muted)) { append("  ▸  ") }
            withStyle(SpanStyle(color = onSurface)) { append(segment) }
        }
    } else {
        val trimmed = destinationDir.trimEnd('/')
        val internal = trimmed.startsWith(EMULATED_STORAGE_PREFIX)
        val tail = if (internal) trimmed.removePrefix(EMULATED_STORAGE_PREFIX).trim('/') else trimmed.trimStart('/')
        val tailSegments = tail.split('/').filter { it.isNotBlank() }
        // Shared storage reads as "Internal"; otherwise fall back to the last couple of path segments.
        val segments = if (internal) {
            listOf(stringResource(R.string.transfer_dest_internal)) + tailSegments.takeLast(1)
        } else {
            tailSegments.takeLast(2)
        }
        segments.forEachIndexed { index, segment ->
            if (index > 0) withStyle(SpanStyle(color = muted)) { append("  ▸  ") }
            withStyle(SpanStyle(color = onSurface)) { append(segment) }
        }
    }
}

private const val EMULATED_STORAGE_PREFIX = "/storage/emulated/0"

// "3 of 5 files · 128 MB / 207 MB · 8.4 MB/s · 12s left"; paused drops speed/ETA, unmeasured shows "Preparing…".
@Composable
private fun activeStats(job: TransferProgress): String {
    if (job.isCounting) return stringResource(R.string.transfer_preparing)
    val parts = mutableListOf<String>()
    if (job.isPaused) parts.add(stringResource(R.string.transfer_paused))
    if (job.totalFiles > 0) {
        parts.add(stringResource(R.string.transfer_files_progress, job.filesDone, job.totalFiles))
    }
    if (job.totalBytes > 0L) {
        parts.add(
            stringResource(
                R.string.transfer_size_progress,
                formatFileSize(job.bytesDone),
                formatFileSize(job.totalBytes),
            ),
        )
    }
    if (!job.isPaused) {
        formatTransferSpeed(job.bytesPerSecond).takeIf { it.isNotEmpty() }?.let { parts.add(it) }
        formatDurationShort(job.secondsRemaining).takeIf { it.isNotEmpty() }
            ?.let { parts.add(stringResource(R.string.transfer_eta, it)) }
    }
    return parts.joinToString("  ·  ")
}

// "Queued · 40 files · 1.2 GB" once measured, else "Queued · 40 items".
@Composable
private fun queuedSubtitle(job: TransferProgress): String {
    val queued = stringResource(R.string.transfer_queued)
    val detail = if (job.totalFiles > 0 && job.totalBytes > 0L) {
        "${stringResource(R.string.transfer_files_total, job.totalFiles)}  ·  ${formatFileSize(job.totalBytes)}"
    } else {
        stringResource(R.string.transfer_item_count, job.itemCount)
    }
    return "$queued  ·  $detail"
}

@Composable
private fun collapsedSubtitle(snapshot: TransferSnapshot): String {
    if (snapshot.isCounting) return stringResource(R.string.transfer_preparing)
    val percent = (snapshot.fraction * 100).roundToInt()
    val parts = mutableListOf(stringResource(R.string.transfer_percent, percent))
    formatTransferSpeed(snapshot.bytesPerSecond).takeIf { it.isNotEmpty() }?.let { parts.add(it) }
    formatDurationShort(snapshot.secondsRemaining).takeIf { it.isNotEmpty() }
        ?.let { parts.add(stringResource(R.string.transfer_eta, it)) }
    return parts.joinToString("  ·  ")
}

private fun ClipboardOperation?.icon(): ImageVector = when (this) {
    ClipboardOperation.MOVE -> Icons.AutoMirrored.Filled.DriveFileMove
    else -> Icons.Filled.ContentCopy
}

@Composable
private fun titleFor(operation: ClipboardOperation?, totalFiles: Int): String = when (operation) {
    ClipboardOperation.MOVE -> stringResource(R.string.transfer_moving, totalFiles)
    else -> stringResource(R.string.transfer_copying, totalFiles)
}

@Composable
private fun failedTitle(count: Int): String = if (count == 1) {
    stringResource(R.string.transfer_failed_title_one)
} else {
    stringResource(R.string.transfer_failed_title_many, count)
}
