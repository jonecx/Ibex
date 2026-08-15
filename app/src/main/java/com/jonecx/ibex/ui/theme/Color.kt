package com.jonecx.ibex.ui.theme

import androidx.compose.ui.graphics.Color

// Monochrome video-first palette, shared with the Azmaree player so both look identical.
val BrandRed = Color(0xFFEF4545) // Matches ic_launcher_background.
val Ink = Color(0xFF1A1A1A)
val Snow = Color(0xFFF2F2F2)
val PaperLight = Color(0xFFFAFAFA)
val PaperDark = Color(0xFF000000)
val MistLight = Color(0xFFE4E4E4)
val MistDark = Color(0xFF2C2C2C)
val Steel = Color(0xFF7A7A7A)
val Slate = Color(0xFF4A4A4A)
val Silver = Color(0xFFC6C6C6)

// Alpha levels (scalars, not colors) reused across composables.
const val AlphaDisabled = 0.5f
const val AlphaSecondary = 0.7f
const val AlphaTintBackground = 0.15f

// Soft resting fill for a grid tile: lifts it off the flat page, stays lighter than the hover or selected state.
const val AlphaTileResting = 0.4f

// Per-category icon accents. Kept vivid and distinct on purpose: file-type and source-tile icons
// stay colour-coded for quick scanning, independent of the monochrome chrome above.
val FileDirectoryColor = Color(0xFFFFB74D)
val FileImageColor = Color(0xFF4CAF50)
val FileVideoColor = Color(0xFFE91E63)
val FileAudioColor = Color(0xFF9C27B0)
val FileDocumentColor = Color(0xFF2196F3)
val FileArchiveColor = Color(0xFF795548)

val SourceStorageColor = Color(0xFF546E7A)
val SourceDownloadsColor = Color(0xFFF57C00)
val SourceImagesColor = Color(0xFFAB47BC)
val SourceVideosColor = Color(0xFFEC407A)
val SourceAudioColor = Color(0xFF26C6DA)
val SourceDocumentsColor = Color(0xFF1976D2)
val SourceAppsColor = Color(0xFF388E3C)
val SourceRecentColor = Color(0xFF78909C)
val SourceAnalysisColor = Color(0xFF8D6E63)
val SourceTrashColor = Color(0xFFD32F2F)
val SourceCloudColor = Color(0xFF42A5F5)
val SourceSmbColor = Color(0xFFE64A19)
val SourceFtpColor = Color(0xFF26A69A)
val SourceLiveColor = Color(0xFFEF5350)
