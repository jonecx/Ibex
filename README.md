<img width="1830" height="1088" alt="Screenshot 2026-03-08 at 6 56 54 PM" src="https://github.com/user-attachments/assets/fa448013-3b87-40ef-9e47-56747f26fce2" />

# Ibex

A modern Android file manager built with Jetpack Compose and Material 3. Browse local storage, connect to SMB network shares, manage files with move/copy/rename/delete, view images with zoom, stream videos with full playback controls, and analyze storage usage — all from a single app.

## Tech Stack

| Layer       | Technology                                                              |
| ----------- | ----------------------------------------------------------------------- |
| UI          | Jetpack Compose, Material 3, Material 3 Adaptive                        |
| Navigation  | Navigation Compose                                                      |
| DI          | Dagger Hilt (constructor injection, multibindings)                      |
| Async       | Kotlin Coroutines + Flow                                                |
| Media       | Media3 ExoPlayer (local + SMB streaming)                                |
| Images      | Coil (with custom SMB fetcher)                                          |
| Network     | jcifs-ng (SMB2/3)                                                       |
| Persistence | DataStore Preferences                                                   |
| Encryption  | Google Tink (credential storage)                                        |
| Analytics   | PostHog                                                                 |
| Logging     | Timber                                                                  |
| Code Style  | Spotless + ktlint                                                       |
| CI          | GitHub Actions (unit + instrumented on Pixel 5 & Pixel 7 Pro emulators) |
| Benchmarks  | Macrobenchmark (startup + scroll), Baseline Profiles                    |

## Requirements

- Android SDK 30+ (Android 11)
- Target SDK 36
- JDK 21
- `MANAGE_EXTERNAL_STORAGE` permission

## Documentation

| Document                             | Description                                            |
| ------------------------------------ | ------------------------------------------------------ |
| [Features](docs/features.md)         | File sources, media viewer, storage analysis, settings |
| [Architecture](docs/architecture.md) | MVVM layers, DI graph, data flow                       |
| [Networking](docs/networking.md)     | SMB implementation, streaming, adding new protocols    |
| [Building](docs/building.md)         | Build variants, dependencies, local properties         |
| [Testing](docs/testing.md)           | Unit, instrumented, screenshot, benchmarks, CI         |

## Project Structure

```
app/src/main/java/com/jonecx/ibex/
  analytics/          # PostHog analytics + Timber bridge
  data/
    crypto/           # Tink encryption for credentials
    model/            # FileItem, FileSource, NetworkConnection, SortOption
    preferences/      # DataStore (settings, network connections)
    repository/       # File repos, SMB, storage analyzer, trash, clipboard
  di/                 # Hilt modules (11 modules)
  logging/            # AppLogger interface + Timber impl
  ui/
    analysis/         # Storage breakdown pie chart
    components/       # Shared UI (dialogs, loading, error, pie chart, tiles)
    explorer/         # File browser (screen, viewmodel, grid/list items, thumbnails)
    home/             # Home screen with source tiles
    navigation/       # NavHost + route definitions
    network/          # Add/edit/list SMB connections
    permission/       # Storage permission flow
    player/           # ExoPlayer + SMB DataSource + playback controls
    settings/         # View mode, grid columns, sort, analytics toggle
    theme/            # Colors, typography, shapes
    viewer/           # Full-screen media viewer (images + video)
  util/               # FileTypeUtils, FormatUtils, MediaStoreUtils
```

## Quick Start

```bash
# Clone and build
git clone https://github.com/jonecx/Ibex.git
cd Ibex
cp local.properties.example local.properties  # or create with empty PostHog keys

# Run all checks (spotless + unit tests + screenshot tests + instrumented tests)
./gradlew sanityCheck

# Unit tests only
./gradlew testDebugUnitTest

# Build and install
./gradlew installDebug

# Run benchmarks (requires device)
./gradlew perfCheck
```

## License

TBD
