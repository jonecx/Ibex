#!/usr/bin/env python3
"""Measure a release APK and emit github-action-benchmark JSON (customSmallerIsBetter)
so APK size trends on the same GitHub Pages site as the macrobenchmarks. Reports the
total plus the dex, resources, and native-lib breakdown that R8 moves most."""

from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path


def measure(apk: Path) -> list[dict]:
    """Total on-disk APK size plus per-category compressed sizes inside it."""
    total = apk.stat().st_size
    dex = resources = native = 0
    with zipfile.ZipFile(apk) as zf:
        for info in zf.infolist():
            name = info.filename
            size = info.compress_size  # compressed = the actual footprint shipped in the APK
            if name.endswith(".dex"):
                dex += size
            elif name == "resources.arsc" or name.startswith("res/"):
                resources += size
            elif name.startswith("lib/"):
                native += size

    return [
        {"name": "APK / total", "unit": "bytes", "value": total},
        {"name": "APK / dex (code)", "unit": "bytes", "value": dex},
        {"name": "APK / resources", "unit": "bytes", "value": resources},
        {"name": "APK / native libs", "unit": "bytes", "value": native},
    ]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, help="Path to the APK to measure")
    parser.add_argument("--output", default="apk-size-gha.json", help="Output JSON file")
    args = parser.parse_args()

    apk = Path(args.apk)
    if not apk.is_file():
        print(f"Error: APK not found: {apk}")
        sys.exit(1)

    entries = measure(apk)
    Path(args.output).write_text(json.dumps(entries, indent=2) + "\n")
    mb = entries[0]["value"] / 1_000_000
    print(f"Wrote {len(entries)} size metrics to {args.output} (total {mb:.1f} MB)")


if __name__ == "__main__":
    main()
