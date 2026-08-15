package com.jonecx.ibex

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.fixtures.runningTransferSnapshot
import com.jonecx.ibex.ui.components.TransferProgressBarContent
import com.jonecx.ibex.ui.theme.IbexTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferProgressBarCollapsedPreview() {
    IbexTheme {
        TransferProgressBarContent(
            snapshot = runningTransferSnapshot(),
            expanded = false,
            onToggleExpanded = {},
            onCancel = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferProgressBarCollapsedDarkPreview() {
    IbexTheme(darkTheme = true) {
        TransferProgressBarContent(
            snapshot = runningTransferSnapshot(),
            expanded = false,
            onToggleExpanded = {},
            onCancel = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferProgressBarExpandedPreview() {
    IbexTheme {
        TransferProgressBarContent(
            snapshot = runningTransferSnapshot(),
            expanded = true,
            onToggleExpanded = {},
            onCancel = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferProgressBarExpandedDarkPreview() {
    IbexTheme(darkTheme = true) {
        TransferProgressBarContent(
            snapshot = runningTransferSnapshot(),
            expanded = true,
            onToggleExpanded = {},
            onCancel = {},
        )
    }
}
