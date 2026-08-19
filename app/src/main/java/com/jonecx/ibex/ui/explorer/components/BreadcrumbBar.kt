package com.jonecx.ibex.ui.explorer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonecx.ibex.R
import com.jonecx.ibex.ui.explorer.Breadcrumb
import com.jonecx.ibex.ui.theme.AlphaSecondary

// Home crumb plus a single current title crumb, for standalone screens that only need a way back to the home screen.
fun singleSourceBreadcrumbs(title: String): List<Breadcrumb> = listOf(
    Breadcrumb(index = Breadcrumb.HOME_INDEX, name = "", isHome = true, isCurrent = false),
    Breadcrumb(index = 0, name = title, isHome = false, isCurrent = true),
)

// Scrollable path bar under the top app bar; the home crumb returns to the home screen, ancestor crumbs jump back up the tree.
@Composable
fun BreadcrumbBar(
    breadcrumbs: List<Breadcrumb>,
    onCrumbClick: (crumb: Breadcrumb) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Keep the current folder in view as the trail grows or shrinks.
    LaunchedEffect(breadcrumbs) {
        if (breadcrumbs.isNotEmpty()) {
            listState.scrollToItem(breadcrumbs.lastIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("breadcrumb_bar"),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        itemsIndexed(
            items = breadcrumbs,
            key = { _, crumb -> crumb.index },
        ) { position, crumb ->
            if (position > 0) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaSecondary),
                    modifier = Modifier.size(18.dp),
                )
            }
            CrumbChip(crumb = crumb, onClick = { onCrumbClick(crumb) })
        }
    }
}

@Composable
private fun CrumbChip(
    crumb: Breadcrumb,
    onClick: () -> Unit,
) {
    val contentColor = if (crumb.isCurrent) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val tag = if (crumb.isHome) "breadcrumb_home" else "breadcrumb_${crumb.name}"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickableWhenAncestor(crumb, onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag),
    ) {
        if (crumb.isHome) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = stringResource(R.string.breadcrumb_home),
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = crumb.name,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = if (crumb.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                // The current crumb repeats the top-bar title, so hide it from TalkBack to avoid a double read.
                modifier = if (crumb.isCurrent) Modifier.clearAndSetSemantics {} else Modifier,
            )
        }
    }
}

// Only ancestors are interactive; the current folder is a plain label.
private fun Modifier.clickableWhenAncestor(crumb: Breadcrumb, onClick: () -> Unit): Modifier =
    if (crumb.isCurrent) this else clickable(onClick = onClick)
