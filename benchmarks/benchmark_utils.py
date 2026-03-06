"""Shared utilities for benchmark result loading and directory discovery."""

import json
from pathlib import Path

RESULTS_DIR_NAME = "results"


def results_dir() -> Path:
    """Return the path to the benchmark results directory."""
    return Path(__file__).parent / RESULTS_DIR_NAME


def list_run_dirs(root: Path | None = None) -> list[Path]:
    """Return all result directories sorted chronologically."""
    root = root or results_dir()
    return sorted(
        [d for d in root.iterdir() if d.is_dir() and d.name != ".gitkeep"],
        key=lambda d: d.name,
    )


def load_benchmarks_from_dir(result_dir: Path) -> dict[str, dict[str, dict]]:
    """Load all benchmark JSON files from a result directory.

    Returns a dict keyed by 'ClassName.testName' with metric dicts containing
    median, minimum, maximum, and runs.
    """
    benchmarks = {}

    for json_file in sorted(result_dir.glob("*.json")):
        try:
            with open(json_file) as f:
                data = json.load(f)
        except (json.JSONDecodeError, IOError):
            continue

        for bench in data.get("benchmarks", []):
            name = bench.get("name", "unknown")
            class_name = bench.get("className", "")
            short_class = class_name.rsplit(".", 1)[-1] if class_name else ""
            key = f"{short_class}.{name}" if short_class else name
            metrics = {}
            for metric_name, metric_data in bench.get("metrics", {}).items():
                metrics[metric_name] = {
                    "median": metric_data.get("median", 0),
                    "minimum": metric_data.get("minimum", 0),
                    "maximum": metric_data.get("maximum", 0),
                    "runs": metric_data.get("runs", []),
                }
            benchmarks[key] = metrics

    return benchmarks


def load_metadata(run_dir: Path) -> dict[str, str]:
    """Load metadata.txt from a run directory into a dict."""
    metadata = {}
    meta_file = run_dir / "metadata.txt"
    if meta_file.exists():
        for line in meta_file.read_text().splitlines():
            if ": " in line:
                k, v = line.split(": ", 1)
                metadata[k.strip()] = v.strip()
    return metadata
