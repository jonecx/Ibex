# Benchmarks

Local-only macrobenchmark suite for tracking Ibex performance over time.
Benchmarks require a **physical device or emulator** — they are not run in CI.

## Prerequisites

- Android device/emulator connected via ADB
- Python 3 (for compare and graph tools)

## Quick Start

```bash
# 1. Run benchmarks (installs app, grants permissions, runs tests)
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest

# 2. Save results with a label
./benchmarks/benchmark_result_collect.sh "before-refactor"

# 3. Make your code changes, then run again
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
./benchmarks/benchmark_result_collect.sh "after-refactor"

# 4. Compare the two runs
python3 benchmarks/benchmark_result_compare.py --latest
```

## Commands

### Run benchmarks + view report (one command)

```bash
./gradlew :macrobenchmark:benchmarkCheck
```

This automatically:

1. Builds and installs the `benchmark` variant of the app
2. Grants `MANAGE_EXTERNAL_STORAGE` via `adb shell appops set`
3. Runs all startup and scroll benchmarks (5 iterations each)
4. Collects results into `benchmarks/results/`
5. Generates and opens the HTML chart report

### Run benchmarks only

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
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

```bash
python3 benchmarks/benchmark_result_chart.py             # generates report.html and opens in browser
python3 benchmarks/benchmark_result_chart.py --no-open   # generates without opening
```

Creates an interactive Chart.js HTML report from all collected runs in `benchmarks/results/`.

### Commit results

```bash
git add benchmarks/results/
git commit -m "benchmark: <description>"
```

Committing results lets you track performance across branches and PRs.

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

## Tips

- Run benchmarks on the **same device** for consistent comparisons
- Close other apps to reduce noise
- Use a label when collecting results so you remember what changed
- The `CompilationNone` vs `BaselineProfile` comparison shows the impact of AOT compilation
