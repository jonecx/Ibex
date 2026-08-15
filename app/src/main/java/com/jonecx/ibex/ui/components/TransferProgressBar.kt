package com.jonecx.ibex.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.data.repository.ClipboardOperation
import com.jonecx.ibex.data.transfer.TransferManager
import com.jonecx.ibex.data.transfer.TransferProgress
import com.jonecx.ibex.data.transfer.TransferSnapshot
import com.jonecx.ibex.data.transfer.TransferStatus
import com.jonecx.ibex.util.formatFileSize
import com.jonecx.ibex.util.formatTransferSpeed
import org.koin.core.context.GlobalContext
import kotlin.math.roundToInt

// Thin entry point: pulls live transfer state from the app-scoped manager. Renders nothing when idle,
// so it can sit unconditionally under any screen's toolbar with zero cost while no transfer is running.
// Resolves Koin directly (not koinInject) so it also no-ops cleanly in previews/screenshots with no graph.
@Composable
fun TransferProgressBar(modifier: Modifier = Modifier) {
    val manager = remember { GlobalContext.getOrNull()?.get<TransferManager>() } ?: return
    val snapshot by manager.snapshot.collectAsState()
    val onCancel = remember(manager) { { id: String -> manager.cancel(id) } }
    var expanded by rememberSaveable { mutableStateOf(false) }
    TransferProgressBarContent(
        snapshot = snapshot,
        expanded = expanded,
        onToggleExpanded = { expanded = !expanded },
        onCancel = onCancel,
        modifier = modifier,
    )
}

// Stateless: expansion is hoisted so both states are directly renderable in previews/tests.
@Composable
fun TransferProgressBarContent(
    snapshot: TransferSnapshot,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!snapshot.hasActive) return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            AggregateProgressLine(fraction = snapshot.fraction, indeterminate = snapshot.totalBytes == 0L)
            AggregateRow(
                snapshot = snapshot,
                expanded = expanded,
                onToggle = onToggleExpanded,
            )
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                snapshot.jobs
                    .filter { it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED }
                    .forEach { job -> TransferJobRow(job = job, onCancel = { onCancel(job.id) }) }
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
private fun AggregateRow(
    snapshot: TransferSnapshot,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val operation = snapshot.jobs.firstOrNull {
        it.status == TransferStatus.RUNNING || it.status == TransferStatus.QUEUED
    }?.operation
    val stateLabel = stringResource(
        if (expanded) R.string.transfer_expanded else R.string.transfer_collapsed,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = stringResource(
                    if (expanded) R.string.transfer_collapse else R.string.transfer_expand,
                ),
                role = Role.Button,
                onClick = onToggle,
            )
            .semantics { stateDescription = stateLabel }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = operation.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = titleFor(operation, snapshot.totalFiles),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitleFor(snapshot),
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

@Composable
private fun TransferJobRow(job: TransferProgress, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.currentFileName ?: titleFor(job.operation, job.totalFiles),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { job.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
private fun subtitleFor(snapshot: TransferSnapshot): String {
    if (snapshot.totalBytes == 0L) return stringResource(R.string.transfer_preparing)
    val percent = (snapshot.fraction * 100).roundToInt()
    val speed = formatTransferSpeed(snapshot.bytesPerSecond)
    val progress = "${formatFileSize(snapshot.bytesDone)} / ${formatFileSize(snapshot.totalBytes)}"
    return if (speed.isEmpty()) "$percent% · $progress" else "$percent% · $progress · $speed"
}
