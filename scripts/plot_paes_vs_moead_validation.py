#!/usr/bin/env python3
"""
Generate an HTML report comparing PAES vs MOEA/D validated fronts on RE/RWA problems.

Each problem gets one row with three scatter plots (rendered with Matplotlib/Seaborn):
  column 1 — reference front
  column 2 — PAES front
  column 3 — MOEA/D front

2D problems use a flat scatter; 3D problems use a 3D scatter (mpl_toolkits.mplot3d).
Plots are rendered as static PNG images and embedded in the HTML as base64, so there
is no browser WebGL-context limit (which prevented many 3D fronts from showing when
the report used Plotly).

The X axis (and, for comparability, the Y/Z axes) share a single range per problem,
taken from the reference front, so the three columns are directly comparable.

Usage:
    python scripts/plot_paes_vs_moead_validation.py \\
        results/validation/PAES_vs_MOEAD/ \\
        resources/referenceFronts/ \\
        [--output results/validation/PAES_vs_MOEAD/report.html]
"""

import argparse
import base64
import io
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # headless backend: render to PNG, never open a window

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from mpl_toolkits.mplot3d import Axes3D  # noqa: F401  (registers the 3d projection)

sns.set_theme(style="whitegrid", context="notebook")

# Problem ordering: 2D first, then 3D
PROBLEMS_2D = ["RE21", "RE22", "RE23", "RE24", "RE25", "RWA1"]
PROBLEMS_3D = [
    "RE31", "RE32", "RE33", "RE34", "RE35", "RE36", "RE37",
    "RWA2", "RWA3", "RWA4", "RWA5", "RWA6", "RWA7",
]
ALL_PROBLEMS = PROBLEMS_2D + PROBLEMS_3D

COLORS = {
    "Reference": "#888888",
    "PAES":      "#1f77b4",
    "MOEA/D":    "#d62728",
}


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def load_front(path: Path) -> np.ndarray | None:
    if not path.exists():
        return None
    return pd.read_csv(path, header=None).values


def axis_ranges(ref: np.ndarray | None, n_obj: int) -> list[tuple[float, float]] | None:
    """Per-axis (min, max) taken from the reference front, shared by the three columns."""
    if ref is None or len(ref) == 0:
        return None
    ranges = []
    for i in range(n_obj):
        lo, hi = float(ref[:, i].min()), float(ref[:, i].max())
        if lo == hi:  # avoid a zero-width axis
            pad = abs(lo) * 0.05 or 1.0
            lo, hi = lo - pad, hi + pad
        ranges.append((lo, hi))
    return ranges


# ---------------------------------------------------------------------------
# Plotting
# ---------------------------------------------------------------------------

def scatter_2d(ax, data, color, title, ranges):
    if data is not None and len(data) > 0:
        ax.scatter(data[:, 0], data[:, 1], s=14, c=color, alpha=0.8, edgecolors="none")
    else:
        ax.text(0.5, 0.5, "no data", ha="center", va="center",
                transform=ax.transAxes, color="#aaa")
    ax.set_title(title, fontsize=12, fontweight="bold")
    ax.set_xlabel("$f_1$")
    ax.set_ylabel("$f_2$")
    if ranges:
        ax.set_xlim(ranges[0])
        ax.set_ylim(ranges[1])


def scatter_3d(ax, data, color, title, ranges):
    if data is not None and len(data) > 0:
        ax.scatter(data[:, 0], data[:, 1], data[:, 2],
                   s=12, c=color, alpha=0.8, edgecolors="none")
    else:
        ax.text2D(0.5, 0.5, "no data", ha="center", va="center",
                  transform=ax.transAxes, color="#aaa")
    ax.set_title(title, fontsize=12, fontweight="bold")
    ax.set_xlabel("$f_1$")
    ax.set_ylabel("$f_2$")
    ax.set_zlabel("$f_3$")
    if ranges:
        ax.set_xlim(ranges[0])
        ax.set_ylim(ranges[1])
        ax.set_zlim(ranges[2])
    ax.view_init(elev=25, azim=45)


def make_problem_png(name, n_obj, ref, paes, moead) -> str:
    """Render the three fronts for one problem as a single PNG, return base64."""
    ranges = axis_ranges(ref, n_obj)
    columns = [
        (ref,   "Reference", COLORS["Reference"]),
        (paes,  "PAES",      COLORS["PAES"]),
        (moead, "MOEA/D",    COLORS["MOEA/D"]),
    ]

    if n_obj == 3:
        fig = plt.figure(figsize=(15, 5))
        for i, (data, label, color) in enumerate(columns, start=1):
            ax = fig.add_subplot(1, 3, i, projection="3d")
            scatter_3d(ax, data, color, label, ranges)
    else:
        fig, axes = plt.subplots(1, 3, figsize=(15, 4.5))
        for ax, (data, label, color) in zip(axes, columns):
            scatter_2d(ax, data, color, label, ranges)

    fig.tight_layout()

    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    plt.close(fig)
    return base64.b64encode(buf.getvalue()).decode("ascii")


# ---------------------------------------------------------------------------
# HTML assembly
# ---------------------------------------------------------------------------

CSS = """
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    margin: 24px 32px;
    background: #fafafa;
    color: #222;
}
h1 { font-size: 22px; margin-bottom: 4px; }
.subtitle { color: #666; font-size: 13px; margin-bottom: 24px; }
.problem-section { margin-bottom: 16px; }
.problem-title {
    font-size: 15px;
    font-weight: 600;
    margin: 16px 0 4px;
    color: #333;
}
.problem-section img {
    width: 100%;
    height: auto;
    background: #fff;
    border: 1px solid #ddd;
    border-radius: 6px;
}
"""


def build_html(results_dir: Path, ref_dir: Path, output_path: Path) -> None:
    rows_html: list[str] = []

    for name in ALL_PROBLEMS:
        is_3d = name in PROBLEMS_3D
        ref_data   = load_front(ref_dir / f"{name}.csv")
        paes_data  = load_front(results_dir / name / "PAES_FUN.csv")
        moead_data = load_front(results_dir / name / "MOEAD_FUN.csv")

        if ref_data is None and paes_data is None and moead_data is None:
            continue

        n_obj = ref_data.shape[1] if ref_data is not None else (3 if is_3d else 2)
        if n_obj > 3:
            continue  # only visualise 2D and 3D problems
        obj_label = f"{n_obj} objectives"

        png_b64 = make_problem_png(name, n_obj, ref_data, paes_data, moead_data)

        rows_html.append(f"""
<div class="problem-section">
  <div class="problem-title">{name} &mdash; {obj_label}</div>
  <img src="data:image/png;base64,{png_b64}" alt="{name} fronts">
</div>""")
        print(f"  rendered {name} ({obj_label})")

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PAES vs MOEA/D — Validation on RE/RWA</title>
  <style>{CSS}</style>
</head>
<body>
  <h1>PAES vs MOEA/D — Validation on RE/RWA problems</h1>
  <p class="subtitle">
    Best configurations from meta-optimization (min HVMinus at evaluation 2000,
    RE3D training set). 10 000 evaluations per run.
    Columns: reference front, PAES, MOEA/D. Axes share the reference-front range
    per problem.
  </p>
  {"".join(rows_html)}
</body>
</html>"""

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(html, encoding="utf-8")
    print(f"Report saved: {output_path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Generate PAES vs MOEA/D validation HTML report (Matplotlib/Seaborn)."
    )
    p.add_argument(
        "results_dir", type=Path,
        help="Directory containing <Problem>/PAES_FUN.csv and MOEAD_FUN.csv",
    )
    p.add_argument(
        "ref_dir", type=Path,
        help="Directory containing reference fronts (e.g. resources/referenceFronts/)",
    )
    p.add_argument(
        "--output", type=Path, default=None,
        help="Output HTML path (default: <results_dir>/report.html)",
    )
    return p.parse_args()


def main() -> None:
    args = parse_args()

    if not args.results_dir.is_dir():
        print(f"ERROR: results directory not found: {args.results_dir}", file=sys.stderr)
        sys.exit(1)
    if not args.ref_dir.is_dir():
        print(f"ERROR: reference fronts directory not found: {args.ref_dir}", file=sys.stderr)
        sys.exit(1)

    output = args.output or (args.results_dir / "report.html")
    build_html(args.results_dir, args.ref_dir, output)


if __name__ == "__main__":
    main()
