#!/usr/bin/env python3
"""
Generate an HTML report comparing PAES_DTLZ, MOEAD_DTLZ and MOEAD_DEFAULT on DTLZ1–7.

Each problem gets one row with four 3D scatter plots:
  column 1 — reference front  (DTLZ<N>.3D.csv)
  column 2 — PAES tuned on DTLZ training set
  column 3 — MOEA/D tuned on DTLZ training set
  column 4 — MOEA/D with default parameters (reference algorithm)

Plots are static PNG images embedded as base64 (no WebGL context limit).
All axes share the range derived from the reference front.

Usage:
    python scripts/plot_dtlz_validation.py \\
        results/validation/PAES_vs_MOEAD_DTLZ/ \\
        resources/referenceFronts/ \\
        [--output results/validation/PAES_vs_MOEAD_DTLZ/report.html]
"""

import argparse
import base64
import io
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from mpl_toolkits.mplot3d import Axes3D  # noqa: F401

sns.set_theme(style="whitegrid", context="notebook")

PROBLEMS = ["DTLZ1", "DTLZ2", "DTLZ3", "DTLZ4", "DTLZ5", "DTLZ6", "DTLZ7"]

COLUMNS = [
    ("Reference",     "REFERENCE",      "#888888"),
    ("PAES (DTLZ)",   "PAES_DTLZ",      "#1f77b4"),
    ("MOEA/D (DTLZ)", "MOEAD_DTLZ",     "#d62728"),
    ("MOEA/D (def.)", "MOEAD_DEFAULT",  "#2ca02c"),
]


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def load_front(path: Path) -> np.ndarray | None:
    if not path.exists():
        return None
    try:
        return pd.read_csv(path, header=None).values
    except Exception:
        return None


def ref_path(ref_dir: Path, name: str) -> Path:
    """DTLZ reference fronts use the .3D suffix."""
    return ref_dir / f"{name}.3D.csv"


def axis_ranges(ref: np.ndarray | None) -> list[tuple[float, float]] | None:
    """Per-axis (min, max) from the reference front."""
    if ref is None or len(ref) == 0:
        return None
    ranges = []
    for i in range(ref.shape[1]):
        lo, hi = float(ref[:, i].min()), float(ref[:, i].max())
        if lo == hi:
            pad = abs(lo) * 0.05 or 1.0
            lo, hi = lo - pad, hi + pad
        ranges.append((lo, hi))
    return ranges


# ---------------------------------------------------------------------------
# Plotting
# ---------------------------------------------------------------------------

def scatter_3d(ax, data, color, title, ranges):
    if data is not None and len(data) > 0:
        ax.scatter(data[:, 0], data[:, 1], data[:, 2],
                   s=10, c=color, alpha=0.8, edgecolors="none")
    else:
        ax.text2D(0.5, 0.5, "no data", ha="center", va="center",
                  transform=ax.transAxes, color="#aaa", fontsize=10)
    ax.set_title(title, fontsize=11, fontweight="bold")
    ax.set_xlabel("$f_1$", fontsize=9)
    ax.set_ylabel("$f_2$", fontsize=9)
    ax.set_zlabel("$f_3$", fontsize=9)
    if ranges:
        ax.set_xlim(ranges[0])
        ax.set_ylim(ranges[1])
        ax.set_zlim(ranges[2])
    ax.view_init(elev=25, azim=45)
    ax.tick_params(labelsize=7)


def make_problem_png(name: str, fronts: dict[str, np.ndarray | None]) -> str:
    """Render 4 subplots for one problem, return base64-encoded PNG."""
    ref = fronts.get("REFERENCE")
    ranges = axis_ranges(ref)

    fig = plt.figure(figsize=(20, 5))
    fig.suptitle(name, fontsize=13, fontweight="bold", y=1.01)

    for i, (label, key, color) in enumerate(COLUMNS, start=1):
        ax = fig.add_subplot(1, 4, i, projection="3d")
        scatter_3d(ax, fronts.get(key), color, label, ranges)

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
h1  { font-size: 22px; margin-bottom: 4px; }
.subtitle { color: #666; font-size: 13px; margin-bottom: 24px; }
.legend {
    display: flex;
    gap: 24px;
    margin-bottom: 20px;
    font-size: 13px;
    align-items: center;
}
.legend-item { display: flex; align-items: center; gap: 6px; }
.swatch {
    width: 16px; height: 16px;
    border-radius: 3px;
    flex-shrink: 0;
}
.problem-section { margin-bottom: 12px; }
.problem-section img {
    width: 100%;
    height: auto;
    background: #fff;
    border: 1px solid #ddd;
    border-radius: 6px;
}
"""

LEGEND_ITEMS = [
    ("Reference front",             "#888888"),
    ("PAES — tuned on DTLZ",       "#1f77b4"),
    ("MOEA/D — tuned on DTLZ",     "#d62728"),
    ("MOEA/D — default parameters", "#2ca02c"),
]


def build_html(results_dir: Path, ref_dir: Path, output_path: Path) -> None:
    legend_html = "\n".join(
        f'<div class="legend-item">'
        f'<div class="swatch" style="background:{color}"></div>{label}</div>'
        for label, color in LEGEND_ITEMS
    )

    rows_html: list[str] = []

    for name in PROBLEMS:
        fronts: dict[str, np.ndarray | None] = {
            "REFERENCE":    load_front(ref_path(ref_dir, name)),
            "PAES_DTLZ":    load_front(results_dir / name / "PAES_DTLZ_FUN.csv"),
            "MOEAD_DTLZ":   load_front(results_dir / name / "MOEAD_DTLZ_FUN.csv"),
            "MOEAD_DEFAULT": load_front(results_dir / name / "MOEAD_DEFAULT_FUN.csv"),
        }

        if all(v is None for v in fronts.values()):
            print(f"  skipping {name}: no data found")
            continue

        png_b64 = make_problem_png(name, fronts)
        counts = {k: (len(v) if v is not None else 0) for k, v in fronts.items()}
        print(f"  rendered {name}: " +
              ", ".join(f"{k}={n}" for k, n in counts.items()))

        rows_html.append(f"""
<div class="problem-section">
  <img src="data:image/png;base64,{png_b64}" alt="{name} fronts">
</div>""")

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>PAES vs MOEA/D — DTLZ Validation</title>
  <style>{CSS}</style>
</head>
<body>
  <h1>PAES vs MOEA/D — Validation on DTLZ1–7</h1>
  <p class="subtitle">
    Configurations tuned on the DTLZ training set (meta-optimization, best HVMinus at
    evaluation 2000). 40&thinsp;000 evaluations per run. 3 objectives.
    Axes share the reference-front range per problem.
  </p>
  <div class="legend">{legend_html}</div>
  {"".join(rows_html)}
</body>
</html>"""

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(html, encoding="utf-8")
    print(f"\nReport saved: {output_path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Generate PAES vs MOEA/D DTLZ validation HTML report."
    )
    p.add_argument("results_dir", type=Path,
                   help="Directory with <Problem>/PAES_DTLZ_FUN.csv, etc.")
    p.add_argument("ref_dir", type=Path,
                   help="Reference-front directory (contains DTLZ1.3D.csv, …)")
    p.add_argument("--output", type=Path, default=None,
                   help="Output HTML (default: <results_dir>/report.html)")
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
