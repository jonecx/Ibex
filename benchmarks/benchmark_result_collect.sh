#!/bin/bash
#
# Collects benchmark results from the latest macrobenchmark run
# and saves them to benchmarks/results/ with git SHA and timestamp.
#
# Usage: ./benchmarks/benchmark_result_collect.sh [optional-label]
# Example: ./benchmarks/benchmark_result_collect.sh "added-baseline-profile"
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$SCRIPT_DIR/results"
SOURCE_DIR="$PROJECT_ROOT/macrobenchmark/build/outputs/connected_android_test_additional_output"

if [ ! -d "$SOURCE_DIR" ]; then
    echo "Error: No benchmark output found at:"
    echo "  $SOURCE_DIR"
    echo ""
    echo "Run benchmarks first:"
    echo "  ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest"
    exit 1
fi

TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
GIT_SHA=$(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo "unknown")
LABEL="${1:-}"

if [ -n "$LABEL" ]; then
    RUN_DIR="$RESULTS_DIR/${TIMESTAMP}_${GIT_SHA}_${LABEL}"
else
    RUN_DIR="$RESULTS_DIR/${TIMESTAMP}_${GIT_SHA}"
fi

mkdir -p "$RUN_DIR"

# Copy all benchmark JSON files
find "$SOURCE_DIR" -name "*.json" -exec cp {} "$RUN_DIR/" \;

# Write metadata
cat > "$RUN_DIR/metadata.txt" <<EOF
timestamp: $TIMESTAMP
git_sha: $GIT_SHA
git_branch: $(git -C "$PROJECT_ROOT" branch --show-current 2>/dev/null || echo "unknown")
label: ${LABEL:-none}
device: $(adb shell getprop ro.product.model 2>/dev/null || echo "unknown")
android_version: $(adb shell getprop ro.build.version.release 2>/dev/null || echo "unknown")
EOF

FILE_COUNT=$(find "$RUN_DIR" -name "*.json" | wc -l | tr -d ' ')
echo "Collected $FILE_COUNT benchmark file(s) to:"
echo "  $RUN_DIR"

# Keep only the 25 most recent runs (FIFO)
MAX_RUNS=25
ALL_RUNS=($(ls -d "$RESULTS_DIR"/*/  2>/dev/null | sort))
NUM_RUNS=${#ALL_RUNS[@]}
if [ "$NUM_RUNS" -gt "$MAX_RUNS" ]; then
    DROP_COUNT=$((NUM_RUNS - MAX_RUNS))
    echo ""
    echo "Pruning $DROP_COUNT old run(s) (keeping $MAX_RUNS most recent)..."
    for (( i=0; i<DROP_COUNT; i++ )); do
        rm -rf "${ALL_RUNS[$i]}"
        echo "  Removed: $(basename "${ALL_RUNS[$i]}")"
    done
fi

echo ""
echo "Commit with:"
echo "  git add benchmarks/results/"
echo "  git commit -m \"benchmark: $TIMESTAMP ($GIT_SHA)\""
