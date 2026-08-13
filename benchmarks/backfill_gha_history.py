#!/usr/bin/env python3
"""
Seeds or updates the github-action-benchmark data.js on gh-pages with runs
from local benchmarks/results/, as a separate device series next to CI's.

Usage:
    ./benchmarks/backfill_gha_history.py --output <path/to/data.js> [--merge <existing data.js>]
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path

from benchmark_to_gha import convert
from benchmark_utils import list_run_dirs, load_metadata

SERIES_NAME = "Ibex Macrobenchmarks (Pixel 7 Pro local)"
DATA_PREFIX = "window.BENCHMARK_DATA = "


def repo_url() -> str:
    """Derive the https repo URL from the origin remote."""
    url = subprocess.run(
        ["git", "remote", "get-url", "origin"],
        capture_output=True, text=True, check=True,
    ).stdout.strip()
    url = url.removesuffix(".git")
    if url.startswith("git@github.com:"):
        url = "https://github.com/" + url.removeprefix("git@github.com:")
    return url


def commit_info(sha: str, url: str) -> dict:
    """Build the commit block github-action-benchmark expects, from git history."""
    fmt = "%H%n%an%n%ae%n%cn%n%ce%n%cI%n%s"
    result = subprocess.run(
        ["git", "show", "-s", f"--format={fmt}", sha],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        return {
            "author": {"name": "unknown", "email": ""},
            "committer": {"name": "unknown", "email": ""},
            "id": sha, "message": "(commit not in local history)",
            "timestamp": "", "url": f"{url}/commit/{sha}",
        }
    full, an, ae, cn, ce, ci, msg = result.stdout.strip().split("\n", 6)
    return {
        "author": {"name": an, "email": ae},
        "committer": {"name": cn, "email": ce},
        "id": full, "message": msg, "timestamp": ci,
        "url": f"{url}/commit/{full}",
    }


def run_date_ms(run_dir: Path, metadata: dict) -> int:
    """Epoch ms of the run, from metadata or the directory name."""
    stamp = metadata.get("timestamp") or "_".join(run_dir.name.split("_")[:2])
    return int(datetime.strptime(stamp, "%Y-%m-%d_%H-%M-%S").timestamp() * 1000)


def build_entries(results: Path, url: str) -> list[dict]:
    entries = []
    for run_dir in list_run_dirs(results):
        benches = convert(run_dir)
        if not benches:
            continue
        metadata = load_metadata(run_dir)
        entries.append({
            "commit": commit_info(metadata.get("git_sha", "unknown"), url),
            "date": run_date_ms(run_dir, metadata),
            "tool": "customSmallerIsBetter",
            "benches": benches,
        })
    return sorted(entries, key=lambda e: e["date"])


def load_data_js(path: Path) -> dict:
    text = path.read_text().strip()
    if text.startswith(DATA_PREFIX):
        text = text.removeprefix(DATA_PREFIX)
    return json.loads(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--results", default=str(Path(__file__).parent / "results"))
    parser.add_argument("--merge", help="Existing data.js to merge into (other series are preserved)")
    parser.add_argument("--output", required=True, help="Where to write data.js")
    args = parser.parse_args()

    url = repo_url()
    new_entries = build_entries(Path(args.results), url)
    if not new_entries:
        print("Error: no runs found to backfill.")
        sys.exit(1)

    data = {"lastUpdate": 0, "repoUrl": url, "entries": {}}
    if args.merge and Path(args.merge).exists():
        data = load_data_js(Path(args.merge))

    # Dedupe by (commit id, run date) so re-running after new local runs only appends.
    existing = data["entries"].get(SERIES_NAME, [])
    seen = {(e["commit"]["id"], e["date"]) for e in existing}
    merged = existing + [e for e in new_entries if (e["commit"]["id"], e["date"]) not in seen]
    data["entries"][SERIES_NAME] = sorted(merged, key=lambda e: e["date"])
    data["lastUpdate"] = max(data["lastUpdate"], max(e["date"] for e in merged))

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(DATA_PREFIX + json.dumps(data, indent=2) + "\n")
    print(f"Wrote {len(merged)} runs ({len(merged) - len(existing)} new) to {out}")


if __name__ == "__main__":
    main()
