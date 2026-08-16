package com.jonecx.ibex

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.fixtures.failedTransferSnapshot
import com.jonecx.ibex.fixtures.pausedTransferSnapshot
import com.jonecx.ibex.fixtures.runningTransferSnapshot
import com.jonecx.ibex.fixtures.sheetTransferSnapshot
import com.jonecx.ibex.ui.components.TransferDetailActions
import com.jonecx.ibex.ui.components.TransferDetailContent
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
            actions = TransferDetailActions.Noop,
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
            actions = TransferDetailActions.Noop,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferProgressBarExpandedPreview() {
    IbexTheme {
        TransferProgressBarContent(
            snapshot = sheetTransferSnapshot(),
            expanded = true,
            onToggleExpanded = {},
            actions = TransferDetailActions.Noop,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferProgressBarExpandedDarkPreview() {
    IbexTheme(darkTheme = true) {
        TransferProgressBarContent(
            snapshot = sheetTransferSnapshot(),
            expanded = true,
            onToggleExpanded = {},
            actions = TransferDetailActions.Noop,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferDetailPausedPreview() {
    IbexTheme {
        TransferDetailContent(snapshot = pausedTransferSnapshot(), actions = TransferDetailActions.Noop)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferDetailPausedDarkPreview() {
    IbexTheme(darkTheme = true) {
        TransferDetailContent(snapshot = pausedTransferSnapshot(), actions = TransferDetailActions.Noop)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferDetailFailedPreview() {
    IbexTheme {
        TransferDetailContent(snapshot = failedTransferSnapshot(), actions = TransferDetailActions.Noop)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TransferDetailFailedDarkPreview() {
    IbexTheme(darkTheme = true) {
        TransferDetailContent(snapshot = failedTransferSnapshot(), actions = TransferDetailActions.Noop)
    }
}
