package com.jonecx.ibex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jonecx.ibex.R

// Shown before a paste enqueues when the drop lands on existing names. One choice applies to the whole
// paste (Keep both / Overwrite / Skip); dismissing leaves the clipboard intact so the user can retry.
@Composable
fun TransferConflictDialog(
    conflictCount: Int,
    sampleName: String,
    onKeepBoth: () -> Unit,
    onOverwrite: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = if (conflictCount == 1) {
        stringResource(R.string.conflict_title_one)
    } else {
        stringResource(R.string.conflict_title_many, conflictCount)
    }
    val message = if (conflictCount == 1) {
        stringResource(R.string.conflict_message_one, sampleName)
    } else {
        stringResource(R.string.conflict_message_many, conflictCount, sampleName)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                ChoiceButton(text = stringResource(R.string.conflict_keep_both), onClick = onKeepBoth)
                ChoiceButton(text = stringResource(R.string.conflict_overwrite), onClick = onOverwrite)
                ChoiceButton(text = stringResource(R.string.conflict_skip), onClick = onSkip)
            }
        },
        // Only Cancel here; the three choices live in the body as full-width, well-separated targets.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ChoiceButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text = text, color = MaterialTheme.colorScheme.primary)
    }
}
