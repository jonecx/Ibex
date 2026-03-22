# Architecture

Ibex follows **MVVM** with a clean separation between UI, ViewModel, and data layers. All cross-cutting concerns are managed through **Dagger Hilt** constructor injection.

## Layers

```
┌─────────────────────────────────────────────┐
│  UI Layer (Compose Screens + Components)    │
│  FileExplorerScreen, HomeScreen, etc.       │
├─────────────────────────────────────────────┤
│  ViewModel Layer                            │
│  FileExplorerViewModel, MediaViewerVM, etc. │
├─────────────────────────────────────────────┤
│  Data Layer                                 │
│  Repositories, Preferences, Crypto          │
├─────────────────────────────────────────────┤
│  DI Layer (Hilt Modules)                    │
│  11 modules binding interfaces to impls     │
└─────────────────────────────────────────────┘
```

### UI Layer

Jetpack Compose screens that observe `StateFlow` from ViewModels. No business logic lives here.

Key screens:
- `HomeScreen` — source tile grid
- `FileExplorerScreen` — file browser with list/grid toggle, selection, sort
- `MediaViewerOverlay` — full-screen image/video pager
- `StorageAnalysisScreen` — pie chart breakdown
- `NetworkConnectionsScreen` / `AddNetworkConnectionScreen` — SMB connection management
- `SettingsScreen` — preferences UI

### ViewModel Layer

Each screen has a dedicated `@HiltViewModel`. ViewModels expose `StateFlow<UiState>` and accept user actions as function calls.

| ViewModel | Responsibilities |
|---|---|
| `FileExplorerViewModel` | File listing, sorting, selection, navigation, create/delete/clipboard |
| `MediaViewerViewModel` | Viewable file list, delete from viewer |
| `StorageAnalysisViewModel` | Trigger storage analysis, expose breakdown |
| `NetworkConnectionsViewModel` | CRUD for saved network connections |
| `SettingsViewModel` | Read/write preferences |

### Data Layer

#### Repositories

| Interface | Implementation | Purpose |
|---|---|---|
| `FileRepository` | `LocalFileRepository` | Local filesystem via `java.io.File` |
| `FileRepository` | `MediaFileRepository` | MediaStore queries (images, videos, audio, documents) |
| `FileRepository` | `SmbFileRepository` | SMB shares via jcifs-ng |
| `FileMoveManager` | `CompositeFileMoveManager` | Routes file ops to protocol handlers |
| `ProtocolFileHandler` | `FileSystemMoveManager` | Local file operations |
| `ProtocolFileHandler` | `SmbFileMoveManager` | SMB file operations |
| `StorageAnalyzer` | `MediaStoreStorageAnalyzer` | Disk usage analysis |
| `FileClipboardManager` | `DefaultFileClipboardManager` | Cut/copy/paste with clipboard state |
| `FileTrashManager` | (concrete) | Android trash API (API 30+) |

#### Preferences

| Interface | Backend | Stores |
|---|---|---|
| `SettingsPreferencesContract` | DataStore | View mode, grid columns, sort option, analytics opt-in |
| `NetworkConnectionsPreferencesContract` | DataStore + Tink | Saved SMB connections with encrypted credentials |

#### Crypto

`CryptoManager` (backed by `TinkCryptoManager`) provides AES-GCM encryption for network credentials stored in DataStore.

## Dependency Injection

11 Hilt modules in `di/`:

| Module | Bindings |
|---|---|
| `RepositoryModule` | File repositories, move managers (multibindings), clipboard, trash |
| `PlayerModule` | `PlayerFactory` -> `ExoPlayerFactory` |
| `ImageLoaderModule` | Coil `ImageLoader` with SMB fetcher |
| `ImageRequestModule` | `FileImageRequestFactory` |
| `PreferencesModule` | Settings + network connection preferences |
| `CryptoModule` | `CryptoManager` -> `TinkCryptoManager` |
| `DispatcherModule` | `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` |
| `AnalyticsModule` | `AnalyticsProvider` -> `PostHogAnalyticsProvider` |
| `LoggerModule` | `AppLogger` -> `TimberLogger` |
| `PermissionModule` | `PermissionChecker` |
| `StorageAnalyzerModule` | `StorageAnalyzer` -> `MediaStoreStorageAnalyzer` |

### Protocol Handler Multibindings

`ProtocolFileHandler` implementations are registered via `@Binds @IntoSet`:

```kotlin
@Binds @IntoSet fun bindFileSystem(impl: FileSystemMoveManager): ProtocolFileHandler
@Binds @IntoSet fun bindSmb(impl: SmbFileMoveManager): ProtocolFileHandler
```

`CompositeFileMoveManager` receives `Set<ProtocolFileHandler>` and routes operations by calling `canHandle(path)` on each handler. Cross-protocol transfers (e.g., SMB to local) are handled via streaming.

## Navigation

Single-Activity architecture with `AppNavigation` defining all routes:

```
HOME -> FILE_EXPLORER (with sourceType, rootPath, title, connectionId args)
HOME -> STORAGE_ANALYSIS
HOME -> NETWORK_CONNECTIONS -> ADD_NETWORK_CONNECTION
HOME -> SETTINGS
FILE_EXPLORER -> MEDIA_VIEWER
```

`MediaViewerArgs` is a singleton that passes viewable files between the explorer and viewer screens without serializing large lists through nav arguments.

## Composition Locals

Three `CompositionLocal` values are provided at the `MainActivity` level:

- `LocalPlayerFactory` — ExoPlayer instance creation
- `LocalMediaViewerArgs` — shared media viewer state
- `LocalFileImageRequestFactory` — Coil image request builder
