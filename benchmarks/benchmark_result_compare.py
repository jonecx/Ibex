#!/usr/bin/env python3
"""
Compares two benchmark result directories and shows performance deltas.

Usage:
    ./benchmarks/benchmark_result_compare.py <baseline_dir> <current_dir>
    ./benchmarks/benchmark_result_compare.py --latest          # compares the two most recent runs
    ./benchmarks/benchmark_result_compare.py --latest 3        # compares run #1 (oldest) vs run #3

Examples:
    ./benchmarks/benchmark_result_compare.py benchmarks/results/2025-03-01_abc1234 benchmarks/results/2025-03-06_def5678
    ./benchmarks/benchmark_result_compare.py --latest
"""

import sys
from pathlib import Path

from benchmark_utils import list_run_dirs, load_benchmarks_from_dir, results_dir


def format_delta(baseline: float, current: float) -> str:
    """Format a delta with direction and percentage."""
    if baseline == 0:
        return "N/A"
    delta = current - baseline
    pct = (delta / baseline) * 100
    sign = "+" if delta > 0 else ""
    arrow = "slower" if delta > 0 else "faster"
    return f"{sign}{delta:.1f}ms ({sign}{pct:.1f}% {arrow})"


def compare(baseline_dir: str, current_dir: str):
    """Compare two benchmark result directories."""
    baseline = load_benchmarks_from_dir(Path(baseline_dir))
    current = load_benchmarks_from_dir(Path(current_dir))

    if not baseline:
        print(f"Error: No benchmark data found in {baseline_dir}")
        sys.exit(1)
    if not current:
        print(f"Error: No benchmark data found in {current_dir}")
        sys.exit(1)

    baseline_name = Path(baseline_dir).name
    current_name = Path(current_dir).name

    print(f"{'=' * 72}")
    print(f"Benchmark Comparison")
    print(f"  Baseline: {baseline_name}")
    print(f"  Current:  {current_name}")
    print(f"{'=' * 72}")

    all_keys = sorted(baseline.keys() | current.keys())
    regressions = 0
    improvements = 0

    for key in all_keys:
        print(f"\n  {key}")
        print(f"  {'-' * 68}")

        base_metrics = baseline.get(key, {})
        curr_metrics = current.get(key, {})
        all_metrics = sorted(base_metrics.keys() | curr_metrics.keys())

        for metric in all_metrics:
            base = base_metrics.get(metric, {})
            curr = curr_metrics.get(metric, {})

            base_median = base.get("median", 0)
            curr_median = curr.get("median", 0)

            if base_median and curr_median:
                delta_str = format_delta(base_median, curr_median)
                print(f"    {metric}:")
                print(f"      baseline: {base_median:.1f}ms  |  current: {curr_median:.1f}ms  |  {delta_str}")
                print(f"      min: {base.get('minimum', 0):.1f} -> {curr.get('minimum', 0):.1f}ms"
                      f"  |  max: {base.get('maximum', 0):.1f} -> {curr.get('maximum', 0):.1f}ms")
                if curr_median > base_median * 1.05:
                    regressions += 1
                elif curr_median < base_median * 0.95:
                    improvements += 1
            elif curr_median:
                print(f"    {metric}: {curr_median:.1f}ms (new)")
            elif base_median:
                print(f"    {metric}: {base_median:.1f}ms (removed)")

    print(f"\n{'=' * 72}")
    print(f"  Summary: {improvements} improved, {regressions} regressed "
          f"(>5% threshold)")
    print(f"{'=' * 72}")


def main():
    if len(sys.argv) >= 2 and sys.argv[1] == "--latest":
        dirs = list_run_dirs()
        if len(dirs) < 2:
            print(f"Error: Need at least 2 result directories to compare.")
            print(f"Found {len(dirs)} in {results_dir()}")
            sys.exit(1)

        if len(sys.argv) >= 3:
            idx = int(sys.argv[2]) - 1
            if idx >= len(dirs):
                print(f"Error: Only {len(dirs)} runs available.")
                sys.exit(1)
            compare(str(dirs[0]), str(dirs[idx]))
        else:
            compare(str(dirs[-2]), str(dirs[-1]))
    elif len(sys.argv) == 3:
        compare(sys.argv[1], sys.argv[2])
    else:
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()
