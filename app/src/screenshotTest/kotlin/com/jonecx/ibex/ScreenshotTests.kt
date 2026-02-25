package com.jonecx.ibex

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.jonecx.ibex.data.model.FileItem
import com.jonecx.ibex.data.model.FileType
import com.jonecx.ibex.ui.explorer.components.DefaultFileImageRequestFactory
import com.jonecx.ibex.ui.explorer.components.FileListItem
import com.jonecx.ibex.ui.explorer.components.LocalFileImageRequestFactory
import com.jonecx.ibex.ui.settings.SettingsScreenContent
import com.jonecx.ibex.ui.settings.SettingsUiState
import com.jonecx.ibex.ui.theme.IbexTheme

@Composable
private fun WithImageRequestFactory(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFileImageRequestFactory provides DefaultFileImageRequestFactory()) {
        content()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FileListItemDocumentPreview() {
    IbexTheme {
        FileListItem(
            fileItem = FileItem(
                name = "report.pdf",
                path = "/storage/emulated/0/Documents/report.pdf",
                uri = Uri.EMPTY,
                size = 2_500_000,
                lastModified = 1700000000000,
                isDirectory = false,
                fileType = FileType.DOCUMENT,
                mimeType = "application/pdf",
            ),
            isSelected = false,
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FileListItemDirectoryPreview() {
    IbexTheme {
        FileListItem(
            fileItem = FileItem(
                name = "Photos",
                path = "/storage/emulated/0/Photos",
                uri = Uri.EMPTY,
                size = 0,
                lastModified = 1700000000000,
                isDirectory = true,
                fileType = FileType.DIRECTORY,
                childCount = 42,
            ),
            isSelected = false,
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FileListItemSelectedPreview() {
    IbexTheme {
        FileListItem(
            fileItem = FileItem(
                name = "song.mp3",
                path = "/storage/emulated/0/Music/song.mp3",
                uri = Uri.EMPTY,
                size = 5_000_000,
                lastModified = 1700000000000,
                isDirectory = false,
                fileType = FileType.AUDIO,
                mimeType = "audio/mpeg",
            ),
            isSelected = true,
            onClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun FileListItemAllTypesPreview() {
    IbexTheme {
        WithImageRequestFactory {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                listOf(
                    FileType.DIRECTORY to "Downloads",
                    FileType.DOCUMENT to "notes.txt",
                    FileType.AUDIO to "track.mp3",
                    FileType.VIDEO to "clip.mp4",
                    FileType.ARCHIVE to "backup.zip",
                    FileType.APK to "app.apk",
                    FileType.UNKNOWN to "data.bin",
                ).forEach { (type, name) ->
                    FileListItem(
                        fileItem = FileItem(
                            name = name,
                            path = "/storage/emulated/0/$name",
                            uri = Uri.EMPTY,
                            size = 1_024_000,
                            lastModified = 1700000000000,
                            isDirectory = type == FileType.DIRECTORY,
                            fileType = type,
                            childCount = if (type == FileType.DIRECTORY) 15 else null,
                        ),
                        isSelected = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenAnalyticsEnabledPreview() {
    IbexTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(sendAnalyticsEnabled = true),
            onNavigateBack = {},
            onAnalyticsToggleChanged = {},
            onViewModeChanged = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsScreenAnalyticsDisabledPreview() {
    IbexTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(sendAnalyticsEnabled = false),
            onNavigateBack = {},
            onAnalyticsToggleChanged = {},
            onViewModeChanged = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FileListItemDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        WithImageRequestFactory {
            FileListItem(
                fileItem = FileItem(
                    name = "photo.jpg",
                    path = "/storage/emulated/0/DCIM/photo.jpg",
                    uri = Uri.EMPTY,
                    size = 3_500_000,
                    lastModified = 1700000000000,
                    isDirectory = false,
                    fileType = FileType.IMAGE,
                    mimeType = "image/jpeg",
                ),
                isSelected = false,
                onClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkThemePreview() {
    IbexTheme(darkTheme = true) {
        SettingsScreenContent(
            uiState = SettingsUiState(sendAnalyticsEnabled = true),
            onNavigateBack = {},
            onAnalyticsToggleChanged = {},
            onViewModeChanged = {},
        )
    }
}
