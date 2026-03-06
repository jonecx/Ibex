#!/usr/bin/env python3
"""
Generates an interactive HTML chart from all benchmark results over time.

Usage:
    ./benchmarks/benchmark_result_chart.py              # generates and opens benchmarks/report.html
    ./benchmarks/benchmark_result_chart.py --no-open    # generates without opening
"""

import json
import subprocess
import sys
from pathlib import Path

from benchmark_utils import list_run_dirs, load_benchmarks_from_dir, load_metadata, results_dir


def load_all_runs() -> list[dict]:
    """Load all benchmark runs sorted chronologically."""
    runs = []
    for run_dir in list_run_dirs():
        benchmarks = {}
        for key, metrics in load_benchmarks_from_dir(run_dir).items():
            for metric_name, metric_data in metrics.items():
                series_key = f"{key} / {metric_name}"
                benchmarks[series_key] = {
                    "median": metric_data.get("median", 0),
                }

        if benchmarks:
            metadata = load_metadata(run_dir)
            label = _format_run_label(run_dir.name, metadata)

            runs.append({
                "dir_name": run_dir.name,
                "label": label,
                "git_sha": metadata.get("git_sha", ""),
                "benchmarks": benchmarks,
            })

    return runs


def _format_run_label(dir_name: str, metadata: dict) -> str:
    """Format a run directory name into a short mm/dd/yy hh:mm label."""
    custom_label = metadata.get("label", "none")
    # Try to parse timestamp from dir name: 2026-03-06_12-29-24_sha[_label]
    try:
        parts = dir_name.split("_")
        date_part = parts[0]  # 2026-03-06
        time_part = parts[1]  # 12-29-24
        y, m, d = date_part.split("-")
        hr, mn, _ = time_part.split("-")
        short = f"{m}/{d}/{y[2:]} {hr}:{mn}"
        if custom_label and custom_label != "none":
            short += f" ({custom_label})"
        return short
    except (IndexError, ValueError):
        if custom_label and custom_label != "none":
            return custom_label
        return dir_name


def get_color(index: int) -> str:
    """Return a distinct color for a chart series."""
    colors = [
        "#4285F4", "#EA4335", "#FBBC04", "#34A853",
        "#FF6D01", "#46BDC6", "#7B61FF", "#F538A0",
        "#00ACC1", "#AB47BC", "#8D6E63", "#78909C",
    ]
    return colors[index % len(colors)]


def generate_html(runs: list[dict], output_path: Path):
    """Generate an HTML file with Chart.js line charts."""
    if not runs:
        print("Error: No benchmark results found.")
        sys.exit(1)

    # Collect all unique series keys
    all_series = set()
    for run in runs:
        all_series.update(run["benchmarks"].keys())
    all_series = sorted(all_series)

    # Group series by benchmark class
    groups: dict[str, list[str]] = {}
    for series in all_series:
        group_name = series.split(".")[0] if "." in series else "Other"
        groups.setdefault(group_name, []).append(series)

    labels_json = json.dumps([r["label"] for r in runs])

    charts_html = ""
    charts_js = ""

    for chart_idx, (group_name, series_list) in enumerate(sorted(groups.items())):
        chart_id = f"chart_{chart_idx}"
        legend_items = []

        datasets_js = ""
        for s_idx, series_key in enumerate(series_list):
            short_name = series_key.split(" / ")
            display_name = short_name[-1] if len(short_name) > 1 else series_key
            # Include the test name for disambiguation
            if len(short_name) > 1:
                test_name = short_name[0].split(".")[-1] if "." in short_name[0] else short_name[0]
                display_name = f"{test_name} ({display_name})"

            color = get_color(s_idx)
            legend_items.append(f'<div class="legend-item"><span class="legend-swatch" style="background:{color}"></span>{display_name}</div>')
            medians = []
            for run in runs:
                data = run["benchmarks"].get(series_key, {})
                medians.append(data.get("median", "null"))

            medians_json = json.dumps(medians)

            datasets_js += f"""
            {{
                label: '{display_name}',
                data: {medians_json},
                borderColor: '{color}',
                backgroundColor: '{color}',
                borderWidth: 2,
                tension: 0.3,
                pointRadius: 4,
                pointHoverRadius: 6,
                fill: false,
            }},
            """

        legend_html = "\n".join(legend_items)
        charts_html += f"""
        <div class="chart-container">
            <h2>{group_name}</h2>
            <div class="canvas-wrap"><canvas id="{chart_id}"></canvas></div>
            <div class="legend-grid">{legend_html}</div>
        </div>
        """

        charts_js += f"""
        new Chart(document.getElementById('{chart_id}'), {{
            type: 'line',
            data: {{
                labels: {labels_json},
                datasets: [{datasets_js}]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                interaction: {{
                    mode: 'index',
                    intersect: false,
                }},
                plugins: {{
                    legend: {{
                        display: false,
                    }},
                    tooltip: {{
                        callbacks: {{
                            label: function(context) {{
                                return context.dataset.label + ': ' + context.parsed.y.toFixed(1) + 'ms';
                            }}
                        }}
                    }}
                }},
                scales: {{
                    y: {{
                        beginAtZero: false,
                        title: {{
                            display: true,
                            text: 'milliseconds'
                        }}
                    }},
                    x: {{
                        title: {{
                            display: true,
                            text: 'Run'
                        }},
                        ticks: {{
                            minRotation: 45,
                            maxRotation: 45,
                        }}
                    }}
                }}
            }}
        }});
        """

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ibex Benchmark Report</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4"></script>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #f5f5f5;
            color: #333;
            padding: 24px;
        }}
        h1 {{
            font-size: 24px;
            margin-bottom: 4px;
        }}
        .subtitle {{
            color: #666;
            font-size: 14px;
            margin-bottom: 24px;
        }}
        .chart-container {{
            background: white;
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 24px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }}
        .chart-container h2 {{
            font-size: 16px;
            margin-bottom: 12px;
            color: #444;
        }}
        .canvas-wrap {{
            height: 360px;
            position: relative;
        }}
        .legend-grid {{
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 6px 24px;
            margin-top: 16px;
            padding-top: 12px;
            border-top: 1px solid #eee;
        }}
        .legend-item {{
            display: flex;
            align-items: center;
            font-size: 13px;
            color: #555;
        }}
        .legend-swatch {{
            display: inline-block;
            width: 14px;
            height: 14px;
            border-radius: 3px;
            margin-right: 8px;
            flex-shrink: 0;
        }}
    </style>
</head>
<body>
    <h1>Ibex Benchmark Report</h1>
    <p class="subtitle">{len(runs)} runs &middot; Generated from benchmarks/results/</p>
    {charts_html}
    <script>{charts_js}</script>
</body>
</html>"""

    output_path.write_text(html)
    print(f"Report generated: {output_path}")


def main():
    output_path = Path(__file__).parent / "report.html"

    runs = load_all_runs()
    generate_html(runs, output_path)

    if "--no-open" not in sys.argv:
        subprocess.run(["open", str(output_path)])


if __name__ == "__main__":
    main()
