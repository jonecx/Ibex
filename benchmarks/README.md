# Benchmarks

Macrobenchmark suite for tracking Ibex performance over time.

CI runs the suite on an emulator on every push to `main` (`.github/workflows/benchmark.yml`) and publishes trend charts to GitHub Pages via [github-action-benchmark](https://github.com/benchmark-action/github-action-benchmark): <https://jonecx.github.io/Ibex/dev/bench/>. Emulator numbers are for trend/regression tracking — for real-world numbers, run locally on a physical device with the tools below.

## Prerequisites

- Android device/emulator connected via ADB
- Python 3 (for compare and graph tools)

## Quick Start

```bash
# 1. Run benchmarks (installs app, grants permissions, runs tests)
./gradlew :macrobenchmark:connectedBenchmarkBenchmarkAndroidTest

# 2. Save results with a label
./benchmarks/benchmark_result_collect.sh "before-refactor"

# 3. Make your code changes, then run again
./gradlew :macrobenchmark:connectedBenchmarkBenchmarkAndroidTest
./benchmarks/benchmark_result_collect.sh "after-refactor"

# 4. Compare the two runs
python3 benchmarks/benchmark_result_compare.py --latest
```

## Commands

### Run benchmarks + compare (one command)

```bash
./gradlew :macrobenchmark:benchmarkCheck
```

This automatically:

1. Builds and installs the `benchmark` variant of the app
2. Grants `MANAGE_EXTERNAL_STORAGE` via `adb shell appops set`
3. Runs all startup and scroll benchmarks (5 iterations each)
4. Collects results into `benchmarks/results/`
5. Compares against the previous run

### Run benchmarks only

```bash
./gradlew :macrobenchmark:connectedBenchmarkBenchmarkAndroidTest
```

This automatically:

1. Builds and installs the `benchmark` variant of the app
2. Grants `MANAGE_EXTERNAL_STORAGE` via `adb shell appops set`
3. Runs all startup and scroll benchmarks (5 iterations each)

### Collect results

```bash
./benchmarks/benchmark_result_collect.sh                        # auto-names with git SHA + timestamp
./benchmarks/benchmark_result_collect.sh "added-lazy-loading"   # add a custom label
```

Copies benchmark JSON files and device metadata into `benchmarks/results/<timestamp>_<git-sha>[_<label>]/`.

### Compare two runs

```bash
python3 benchmarks/benchmark_result_compare.py --latest                                                  # two most recent
python3 benchmarks/benchmark_result_compare.py benchmarks/results/DIR_A benchmarks/results/DIR_B         # specific runs
```

Prints a table with median, min, max, and percentage delta for each metric. Flags regressions (>5% slower) and improvements (>5% faster).

### Graph trends over time

Trend charts live on GitHub Pages, updated by CI on every push to `main`: <https://jonecx.github.io/Ibex/dev/bench/>. Local results in `benchmarks/results/` are gitignored and stay on your machine.

### Publish local device runs to the charts

Local runs appear on the same page as their own series ("Pixel 7 Pro local"), kept separate from the emulator series. To append new local runs:

```bash
git worktree add /tmp/gh-pages gh-pages
python3 benchmarks/backfill_gha_history.py --merge /tmp/gh-pages/dev/bench/data.js --output /tmp/gh-pages/dev/bench/data.js
git -C /tmp/gh-pages commit -am "append local benchmark runs" && git -C /tmp/gh-pages push
git worktree remove /tmp/gh-pages
```

Already-published runs are deduped, so this is safe to re-run anytime.

## Tests

| Test                              | Metric                   | What it measures                               |
| --------------------------------- | ------------------------ | ---------------------------------------------- |
| `startupCold`                     | `timeToInitialDisplayMs` | Cold start — app process not running           |
| `startupWarm`                     | `timeToInitialDisplayMs` | Warm start — process alive, activity recreated |
| `startupCompilationNone`          | `timeToInitialDisplayMs` | Cold start with no AOT compilation             |
| `scrollImagesGridCompilationNone` | `frameDurationCpuMs`     | Images grid scroll, no AOT                     |
| `scrollImagesGridBaselineProfile` | `frameDurationCpuMs`     | Images grid scroll, with baseline profile      |
| `scrollVideosGridCompilationNone` | `frameDurationCpuMs`     | Videos grid scroll, no AOT                     |
| `scrollVideosGridBaselineProfile` | `frameDurationCpuMs`     | Videos grid scroll, with baseline profile      |

## Output Format

Each collected run creates a directory in `results/` containing:

- **Benchmark JSON files** — raw metrics (median, min, max, individual iteration data)
- **metadata.txt** — device model, Android version, git SHA, branch, and optional label

## APK size

CI also tracks the minified release APK size on every push to `main` (`.github/workflows/apk-size.yml`) and publishes trend charts to the same GitHub Pages site: <https://jonecx.github.io/Ibex/dev/apk-size/>. It charts the total APK plus the dex, resources, and native-lib breakdown that R8 moves most (smaller-is-better, bytes). Both publishers share one concurrency group so gh-pages writes never race.

Measure locally against any built APK:

```bash
./gradlew :app:assembleRelease
python3 benchmarks/apk_size_to_gha.py --apk app/build/outputs/apk/release/app-release.apk
```

## Tips

- Run benchmarks on the **same device** for consistent comparisons
- Close other apps to reduce noise
- Use a label when collecting results so you remember what changed
- The `CompilationNone` vs `BaselineProfile` comparison shows the impact of AOT compilation
