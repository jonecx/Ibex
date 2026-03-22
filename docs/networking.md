# Networking

## SMB (Server Message Block)

Ibex connects to SMB/CIFS network shares using [jcifs-ng](https://github.com/AgNO3/jcifs-ng) (2.1.10).

### Connection Setup

1. User adds a connection via **Network Connections** screen (host IP, port, credentials)
2. Credentials are encrypted with Google Tink and persisted in DataStore
3. On first access, `SmbFileRepository` creates a `CIFSContext` with:
   - SMB2/3 only (min: `SMB202`, max: `SMB311`)
   - 30s response timeout, 35s socket timeout
4. `SmbContextProvider` caches `CIFSContext` instances by host in a `ConcurrentHashMap`

### File Browsing

`SmbFileRepository` implements `FileRepository` and lists directories via `SmbFile.listFiles()`. Each `SmbFile` is mapped to a `FileItem` using `FileTypeUtils.SmbFile.toFileItem()`.

### File Operations

`SmbFileMoveManager` implements `ProtocolFileHandler` for all `smb://` paths:

| Operation | Implementation |
|---|---|
| Move | Copy to destination + delete source |
| Copy | `SmbFile.copyTo()` for files, recursive for directories |
| Rename | `SmbFile.renameTo()` |
| Create Folder | `SmbFile.mkdir()` |
| Delete | `SmbFile.delete()`, recursive for directories |

Recursive operations (`deleteDirectoryRecursive`, `copyDirectoryRecursive`) are `suspend` functions with `ensureActive()` checks for coroutine cancellation.

### Cross-Protocol Transfers

`CompositeFileMoveManager` handles transfers between different protocols (e.g., local to SMB) by:

1. Opening an `InputStream` on the source handler
2. Opening an `OutputStream` on the destination handler
3. Streaming with `copyTo()` using a 64KB buffer
4. For directories, recursively listing and transferring each child

### Thumbnails

`SmbThumbnailFetcher` is a custom Coil `Fetcher` for `smb://` paths:

- **Images** — streams directly from SMB into an okio `Buffer`
- **Videos** — downloads first 5MB to a temp file, extracts a frame via FFmpeg at 1s (retry at 0s)
- Concurrency limited to 3 simultaneous fetches via `Semaphore`

### Video Streaming

SMB videos are streamed directly through ExoPlayer without downloading:

- `SmbDataSource` extends Media3 `BaseDataSource` and uses `SmbRandomAccessFile` for seek-capable streaming
- `SmbAwareDataSourceFactory` routes `smb://` URIs to `SmbDataSource`, all other schemes to `DefaultDataSource`
- `ExoPlayerFactory` builds every ExoPlayer instance with this composite data source factory

## Adding a New Protocol

The architecture is designed so new protocols (FTP, WebDAV, etc.) can be added with minimal changes:

### 1. Implement `ProtocolFileHandler`

```kotlin
@Singleton
class FtpFileMoveManager @Inject constructor(
    // ... FTP-specific dependencies
) : ProtocolFileHandler {
    override fun canHandle(path: String): Boolean = path.startsWith("ftp://")
    override suspend fun moveFile(fileItem: FileItem, destinationDir: String): Boolean { /* ... */ }
    override suspend fun copyFile(fileItem: FileItem, destinationDir: String): Boolean { /* ... */ }
    override suspend fun renameFile(fileItem: FileItem, newName: String): Boolean { /* ... */ }
    override suspend fun createFolder(parentDir: String, name: String): Boolean { /* ... */ }
    override suspend fun deleteFile(fileItem: FileItem): Boolean { /* ... */ }
    override suspend fun openInputStream(path: String): InputStream { /* ... */ }
    override suspend fun openOutputStream(path: String): OutputStream { /* ... */ }
    override suspend fun listFiles(path: String): List<FileItem> { /* ... */ }
}
```

### 2. Register in `RepositoryModule`

```kotlin
@Binds @IntoSet
abstract fun bindFtpHandler(impl: FtpFileMoveManager): ProtocolFileHandler
```

### 3. Done

`CompositeFileMoveManager` automatically discovers the new handler via Dagger multibindings. Cross-protocol transfers (e.g., FTP to SMB) work immediately through the streaming bridge.

### Optional: Video Streaming

To add streaming for the new protocol, create a `DataSource` implementation and add a scheme check in `SmbAwareDataSource.open()`.

### Optional: Thumbnails

Create a Coil `Fetcher` for the new scheme and register it in `ImageLoaderModule`.
