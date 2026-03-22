# Testing

## Overview

Ibex has three test source sets plus a macrobenchmark module:

| Source Set         | Runner             | Location                  |
| ------------------ | ------------------ | ------------------------- |
| Unit tests         | JVM (Robolectric)  | `app/src/test/`           |
| Instrumented tests | Device/Emulator    | `app/src/androidTest/`    |
| Screenshot tests   | Compose Screenshot | `app/src/screenshotTest/` |
| Benchmarks         | Macrobenchmark     | `macrobenchmark/`         |

Shared fakes live in `app/src/testFixtures/` and are available to all test source sets.

## Running Tests

```bash
# Everything (spotless + unit + screenshot + instrumented)
./gradlew sanityCheck

# Unit tests only
./gradlew testDebugUnitTest

# Instrumented tests only (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Screenshot tests
./gradlew validateDebugScreenshotTest

# Update screenshot references
./gradlew updateDebugScreenshotTest
```

## Unit Tests (`app/src/test/`)

JVM-based tests using JUnit 4, Robolectric (for Android framework APIs), Turbine (for Flow testing), and `kotlinx-coroutines-test`.

### Test Coverage

| Area               | Tests                                                                                                           |
| ------------------ | --------------------------------------------------------------------------------------------------------------- |
| `data/repository/` | `CompositeFileMoveManagerTest`, `SmbFileRepositoryTest`, `LocalFileRepositoryTest`, `RecentFilesRepositoryTest` |
| `data/model/`      | `SortOptionTest`, `FileSourcesTest`                                                                             |
| `ui/explorer/`     | `FileExplorerViewModelTest`, `FileExplorerScreenTest`                                                           |
| `ui/viewer/`       | `MediaViewerViewModelTest`, `MediaViewerArgsTest`                                                               |
| `ui/analysis/`     | `StorageAnalysisViewModelTest`                                                                                  |
| `ui/network/`      | `NetworkConnectionsViewModelTest`                                                                               |
| `ui/settings/`     | `SettingsViewModelTest`                                                                                         |
| `util/`            | `FileTypeUtilsTest`, `FileTypeUtilsSmbTest`, `FormatUtilsTest`                                                  |
| `analytics/`       | `AnalyticsManagerTest`, `AnalyticsTreeTest`                                                                     |

## Instrumented Tests (`app/src/androidTest/`)

On-device tests using Compose Test Rules, Hilt testing, and Espresso.

### Test Coverage

| Area               | Tests                                                                                                         |
| ------------------ | ------------------------------------------------------------------------------------------------------------- |
| `ui/explorer/`     | `FileExplorerScreenTest`, `SmbFileExplorerTest`, `FileListItemTest`, `FileGridItemTest`, `FileDetailPaneTest` |
| `ui/player/`       | `VideoPlayerIntegrationTest` (28 tests: playback, controls, speed, paging, delete)                            |
| `ui/viewer/`       | `MediaViewerNavigationTest`                                                                                   |
| `ui/home/`         | `HomeScreenTest`                                                                                              |
| `ui/analysis/`     | `StorageAnalysisScreenTest`                                                                                   |
| `ui/network/`      | `NetworkConnectionsScreenTest`, `AddNetworkConnectionScreenTest`, `NetworkConnectionScreenEditTest`           |
| `ui/settings/`     | `SettingsScreenTest`                                                                                          |
| `ui/navigation/`   | `AppNavigationTest`                                                                                           |
| `data/repository/` | `FileOperationsIntegrationTest` (move, copy, rename, create folder, delete with real filesystem)              |
| `util/`            | `FormatUtilsInstrumentedTest`, `MediaStoreUtilsInstrumentedTest`                                              |

### Hilt Test Configuration

Instrumented tests use `HiltTestRunner` (configured in `build.gradle.kts` as `testInstrumentationRunner`). Fake modules in `app/src/androidTest/java/com/jonecx/ibex/di/` replace production bindings with test doubles.

## Test Fixtures (`app/src/testFixtures/`)

Shared fake implementations used by both unit and instrumented tests:

| Fake                                | Replaces                                                                          |
| ----------------------------------- | --------------------------------------------------------------------------------- |
| `FakeFileRepository`                | `FileRepository`                                                                  |
| `FakeFileRepositoryFactory`         | `FileRepositoryFactory`                                                           |
| `FakeFileMoveManager`               | `FileMoveManager` / `ProtocolFileHandler`                                         |
| `FakeFileTrashManager`              | `FileTrashManager`                                                                |
| `FakeFileClipboardManager`          | `FileClipboardManager`                                                            |
| `FakeSettingsPreferences`           | `SettingsPreferencesContract`                                                     |
| `FakeNetworkConnectionsPreferences` | `NetworkConnectionsPreferencesContract`                                           |
| `FakeSmbContextProvider`            | `SmbContextProviderContract`                                                      |
| `FakePlayerFactory`                 | `PlayerFactory`                                                                   |
| `FakeStorageAnalyzer`               | `StorageAnalyzer`                                                                 |
| `FakeAnalyticsProvider`             | `AnalyticsProvider`                                                               |
| `FakePermissionChecker`             | `PermissionChecker`                                                               |
| `TestFileItems`                     | Provides `testFileItem()`, `testImageFileItem()`, `testRemoteFileItem()` builders |

## Screenshot Tests (`app/src/screenshotTest/`)

Compose Preview Screenshot Testing validates UI components against reference images. Tests are `@Composable` functions annotated with `@PreviewTest` + `@Preview`.

### Test Coverage

| Test Class                          | Previews                                                  |
| ----------------------------------- | --------------------------------------------------------- |
| `FileExplorerScreenshotTests`       | Document, directory, selected, all file types, dark theme |
| `NetworkConnectionsScreenshotTests` | Connection list, add connection form                      |
| `SettingsScreenshotTests`           | Settings screen states                                    |
| `StorageAnalysisScreenshotTests`    | Pie chart with category breakdown                         |

### Running Screenshot Tests

```bash
# Validate against reference images
./gradlew validateDebugScreenshotTest

# Update reference images after intentional UI changes
./gradlew updateDebugScreenshotTest
```

Reference images are stored in `app/src/screenshotTest/` and checked into source control. The `screenshot-tests.yml` CI workflow validates them on every push/PR to `main`.

## Baseline Profile

The baseline profile is **not a test** — it's a code path recorder. `BaselineProfileGenerator` (in `macrobenchmark/`) launches the app and exercises critical user journeys:

1. **Cold start** — captures startup code paths
2. **Grid view toggle** — captures settings + preference code paths
3. **Scroll Images** — captures file explorer, Coil thumbnails, LazyVerticalGrid
4. **Scroll Videos** — captures video thumbnail loading

ART traces which classes and methods are touched and outputs a `.prof` file. When the app is installed on a user's device, ART uses this profile to **AOT-compile those hot paths ahead of time** instead of waiting for JIT. Result: faster startup and smoother scrolling on first launch.

This is why `BaselineProfileGenerator` is **SKIPPED** during benchmark runs — it's only meant to run when generating/updating the profile, not measuring performance.

```bash
# Generate the baseline profile (requires connected device)
./gradlew :app:generateBaselineProfile
```

## Benchmarks

Two benchmark classes in `macrobenchmark/` measure real device performance:

- **`StartupBenchmark`** — measures `timeToInitialDisplayMs` (how fast the app renders its first frame)
  - `startupColdNoCompilation` — worst case: no AOT, no baseline profile, pure JIT
  - `startupColdBaselineProfile` — with `CompilationMode.Partial()` (baseline profile applied)
  - `startupWarm` — app process already exists, just creates a new Activity
- **`ScrollBenchmark`** — measures `FrameTimingMetric` (frame count, duration, jank) while fling-scrolling the grid
  - Images + Videos × `CompilationNone` vs `BaselineProfile`

Each test runs multiple iterations for statistical reliability.

### Running Benchmarks

```bash
# All-in-one: run benchmarks, collect results, compare with previous run, generate HTML chart
./gradlew perfCheck

# Or run individual steps:
./gradlew :macrobenchmark:connectedBenchmarkBenchmarkAndroidTest

cd benchmarks
./benchmark_result_collect.sh
python benchmark_result_compare.py
python benchmark_result_chart.py
```

Results are stored in `benchmarks/results/` (FIFO-pruned to 25 runs).

> **Note:** Benchmarks require a physical device or emulator. They are not run on CI.

### How the Chart Connects

The chart at `benchmarks/report.html` plots **median values from each benchmark run over time**. Every time you run `./gradlew perfCheck`:

1. Benchmarks execute → raw JSON results saved
2. `benchmark_result_collect.sh` → copies results to `benchmarks/results/{timestamp}/`
3. `benchmark_result_compare.py` → diffs latest vs previous run (shows % deltas)
4. `benchmark_result_chart.py` → reads **all** stored runs and plots trend lines, then opens/refreshes the report in the browser

The chart answers: **"Is the app getting faster or slower over time?"** The `CompilationNone` vs `BaselineProfile` pairs show exactly how much the profile helps — if those lines diverge, the profile is doing its job.

## CI

GitHub Actions workflows in `.github/workflows/`:

| Workflow               | Trigger           | Steps                                                                                              |
| ---------------------- | ----------------- | -------------------------------------------------------------------------------------------------- |
| `ibex-ci.yml`          | Push/PR to `main` | Unit tests on Ubuntu, then instrumented tests on Pixel 5 (API 30) + Pixel 7 Pro (API 34) emulators |
| `spotless.yml`         | Push/PR to `main` | Code formatting check                                                                              |
| `screenshot-tests.yml` | Push/PR to `main` | Compose screenshot validation                                                                      |
