# Features

## Home Screen

The home screen presents all file sources as a grid of tiles, organized into **Local** and **Remote** sections.

### Local Sources

| Source | Description |
|---|---|
| **Storage** | Browse the full device filesystem from root |
| **Downloads** | Quick access to the Downloads directory |
| **Images** | MediaStore-backed image gallery |
| **Videos** | MediaStore-backed video gallery |
| **Audio** | MediaStore-backed audio files |
| **Documents** | MediaStore-backed documents (PDF, DOCX, XLSX, etc.) |
| **Apps** | Lists installed applications with package info |
| **Recent** | Recently accessed files via MediaStore |
| **Trash** | View and restore trashed files (Android 11+) |
| **Storage Analysis** | Pie chart breakdown of disk usage by category |

### Remote Sources

| Source | Description |
|---|---|
| **SMB** | Browse SMB/CIFS network shares (SMB2/3) |
| **FTP** | Placeholder for future FTP support |
| **Cloud** | Placeholder for future cloud provider support |

## File Explorer

The file explorer supports both local and SMB remote filesystems with a shared UI.

### Browsing

- **List and Grid views** with configurable column count (2-6)
- **Sorting** by name, size, date modified, or date created (ascending/descending)
- **Folder navigation** with breadcrumb-style back navigation
- **Thumbnails** for images, videos, and GIFs (local via Coil, SMB via custom `SmbThumbnailFetcher`)

### File Operations

All operations work on both local and SMB files, including cross-protocol transfers (e.g., copy from SMB to local):

- **Move** — relocate files/directories to a new location
- **Copy** — duplicate files/directories
- **Rename** — change file or directory names
- **Create Folder** — create new directories
- **Delete** — remove files/directories (local files go to trash, remote files are permanently deleted)
- **Cut/Copy + Paste** — clipboard-based file operations with multi-file support

### Selection Mode

Long-press any file to enter selection mode. Supports multi-select for batch operations.

## Media Viewer

Full-screen media viewer with horizontal paging between files.

### Images

- Pinch-to-zoom and pan with `ZoomableImage`
- Supports all image formats Coil handles (JPEG, PNG, WebP, GIF, etc.)

### Video Player

- Powered by Media3 ExoPlayer with Compose UI
- **SMB streaming** — videos from network shares stream directly without downloading, using a custom `SmbDataSource` with seek support via `SmbRandomAccessFile`
- Playback controls: play/pause, skip forward/back, seek bar, previous/next
- Variable playback speed (0.25x - 2x)
- Tap to toggle controls visibility

## Storage Analysis

Analyzes device storage usage by querying MediaStore and StorageStatsManager:

- Interactive pie chart showing category breakdown
- Categories: Images, Videos, Audio, Documents, Apps, Other
- Total/used/free space display

## Network Connections

Manage saved SMB server connections:

- Add connections with host (IP octets), port, display name, and credentials
- Anonymous login support
- Edit and delete existing connections
- Credentials encrypted at rest using Google Tink

## Settings

- **View Mode** — switch between list and grid
- **Grid Columns** — adjustable from 2 to 6 columns
- **Sort Order** — field and direction
- **Analytics** — opt-in/out of PostHog analytics
