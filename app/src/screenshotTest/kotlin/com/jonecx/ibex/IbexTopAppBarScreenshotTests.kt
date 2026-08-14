package com.jonecx.ibex

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.ui.components.IbexTopAppBar
import com.jonecx.ibex.ui.theme.IbexTheme

private const val LONG_TITLE =
    ".td21709d_0F94FB1728313F74A403B19908EFDB6795A0E8BA5B0674650429653B8C4827B"

// Guards the top bar against wrapping: a long name must stay one line and end in an ellipsis.
@PreviewTest
@Preview(showBackground = true)
@Composable
fun IbexTopAppBarLongTitlePreview() {
    IbexTheme {
        IbexTopAppBar(
            title = LONG_TITLE,
            onNavigateBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                }
            },
        )
    }
}
