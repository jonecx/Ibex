package com.jonecx.ibex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.data.model.FileSource
import com.jonecx.ibex.ui.theme.AlphaDisabled
import com.jonecx.ibex.ui.theme.AlphaTintBackground

@Composable
fun SourceTile(
    source: FileSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = source.isEnabled, role = Role.Button, onClick = onClick)
            .semantics {
                // One spoken label for the whole tile so TalkBack reads it once, in words, not icon + text + shorthand.
                contentDescription = source.contentDescription ?: source.name
                // Announce unavailable sources as disabled instead of leaving the dim colour as the only cue.
                if (!source.isEnabled) disabled()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (source.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaDisabled)
            },
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            // Min height (not fixed) so the tile grows instead of clipping when the user scales font size up.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(source.iconTint.copy(alpha = AlphaTintBackground)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = source.icon,
                        // Decorative: the tile's own contentDescription already names the source.
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (source.isEnabled) source.iconTint else source.iconTint.copy(alpha = AlphaDisabled),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (source.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = AlphaDisabled)
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                source.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (source.isEnabled) 1f else AlphaDisabled,
                        ),
                        textAlign = TextAlign.Center,
                        // Two lines so the size/count still shows in full when the user scales font size up.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
