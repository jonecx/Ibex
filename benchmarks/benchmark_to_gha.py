#!/usr/bin/env python3
"""
Converts macrobenchmark benchmarkData.json output into the JSON format
consumed by benchmark-action/github-action-benchmark (customSmallerIsBetter).

Usage:
    ./benchmarks/benchmark_to_gha.py [--input <dir>] [--output <file>]
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

DEFAULT_INPUT = "macrobenchmark/build/outputs/connected_android_test_additional_output"
DEFAULT_OUTPUT = "benchmark-gha.json"

# Sampled-metric percentiles worth charting over time.
PERCENTILES = ("P50", "P90", "P99")


def convert(input_dir: Path) -> list[dict]:
    """Flatten all ms-based metrics into smaller-is-better chart entries."""
    entries = []
    for json_file in sorted(input_dir.rglob("*benchmarkData.json")):
        try:
            data = json.loads(json_file.read_text())
        except (json.JSONDecodeError, OSError):
            continue

        for bench in data.get("benchmarks", []):
            class_name = bench.get("className", "").rsplit(".", 1)[-1]
            test_name = bench.get("name", "unknown")
            name = f"{class_name}.{test_name}" if class_name else test_name

            # frameCount and other non-ms metrics are skipped: this format is smaller-is-better only.
            for metric, values in bench.get("metrics", {}).items():
                if not metric.endswith("Ms"):
                    continue
                entries.append({
                    "name": f"{name} / {metric} (median)",
                    "unit": "ms",
                    "value": values.get("median", 0),
                })

            for metric, values in bench.get("sampledMetrics", {}).items():
                if not metric.endswith("Ms"):
                    continue
                for pct in PERCENTILES:
                    if pct in values:
                        entries.append({
                            "name": f"{name} / {metric} {pct}",
                            "unit": "ms",
                            "value": values[pct],
                        })

    return entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", default=DEFAULT_INPUT, help="Directory containing *benchmarkData.json")
    parser.add_argument("--output", default=DEFAULT_OUTPUT, help="Output JSON file")
    args = parser.parse_args()

    input_dir = Path(args.input)
    if not input_dir.is_dir():
        print(f"Error: input directory not found: {input_dir}")
        sys.exit(1)

    entries = convert(input_dir)
    if not entries:
        print(f"Error: no benchmark metrics found under {input_dir}")
        sys.exit(1)

    Path(args.output).write_text(json.dumps(entries, indent=2) + "\n")
    print(f"Wrote {len(entries)} metrics to {args.output}")


if __name__ == "__main__":
    main()
