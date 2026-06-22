#!/usr/bin/env python3
"""
Plot a single Pareto front (a FUN.csv produced by a base-level Evolver run), optionally overlaying
its reference front.

The number of objectives is auto-detected from the column count: 2 -> 2D scatter, 3 -> 3D scatter,
more than 3 -> parallel coordinates.

Usage:
    python scripts/plot_front.py <FUN.csv> [<referenceFront.csv>] [--output figure.png] [--title TITLE]

By default the figure is written next to the FUN file (<FUN_stem>.png).
"""

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402
from mpl_toolkits.mplot3d import Axes3D  # noqa: F401,E402  (registers the 3d projection)

FRONT_COLOR = "#1f77b4"
REF_COLOR = "#cccccc"


def load_front(path):
    if path is None or not Path(path).exists():
        return None
    return pd.read_csv(path, header=None).values


def plot_2d(ax, front, ref, title):
    if ref is not None and len(ref):
        ax.scatter(ref[:, 0], ref[:, 1], s=8, c=REF_COLOR, label="reference", edgecolors="none")
    ax.scatter(front[:, 0], front[:, 1], s=14, c=FRONT_COLOR, label="front", edgecolors="none")
    ax.set_xlabel("$f_1$")
    ax.set_ylabel("$f_2$")
    ax.set_title(title)
    ax.legend(fontsize=8)


def plot_3d(fig, front, ref, title):
    ax = fig.add_subplot(111, projection="3d")
    if ref is not None and len(ref):
        ax.scatter(ref[:, 0], ref[:, 1], ref[:, 2], s=6, c=REF_COLOR,
                   label="reference", edgecolors="none")
    ax.scatter(front[:, 0], front[:, 1], front[:, 2], s=12, c=FRONT_COLOR,
               label="front", edgecolors="none")
    ax.set_xlabel("$f_1$")
    ax.set_ylabel("$f_2$")
    ax.set_zlabel("$f_3$")
    ax.set_title(title)
    ax.legend(fontsize=8)
    ax.view_init(elev=25, azim=45)


def plot_parallel(ax, front, title):
    n_obj = front.shape[1]
    xs = list(range(n_obj))
    for row in front:
        ax.plot(xs, row, color=FRONT_COLOR, alpha=0.4, lw=0.8)
    ax.set_xticks(xs)
    ax.set_xticklabels([f"$f_{{{i + 1}}}$" for i in range(n_obj)])
    ax.set_title(title)


def main():
    parser = argparse.ArgumentParser(
        description="Plot a single Pareto front (optionally vs its reference)."
    )
    parser.add_argument("front", type=Path, help="FUN.csv with the objective values")
    parser.add_argument("reference", type=Path, nargs="?", default=None,
                        help="reference front CSV (optional)")
    parser.add_argument("--output", type=Path, default=None,
                        help="output image (default: <front>.png)")
    parser.add_argument("--title", default=None, help="figure title")
    args = parser.parse_args()

    if not args.front.exists():
        print(f"ERROR: front file not found: {args.front}", file=sys.stderr)
        sys.exit(1)

    front = load_front(args.front)
    ref = load_front(args.reference)
    title = args.title or args.front.stem
    n_obj = front.shape[1]

    if n_obj == 3:
        fig = plt.figure(figsize=(7, 6))
        plot_3d(fig, front, ref, title)
    else:
        fig, ax = plt.subplots(figsize=(7, 6))
        if n_obj == 2:
            plot_2d(ax, front, ref, title)
        else:
            if ref is not None:
                print("note: reference ignored for >3 objectives (parallel coordinates)",
                      file=sys.stderr)
            plot_parallel(ax, front, title)

    fig.tight_layout()
    output = args.output or args.front.with_suffix(".png")
    fig.savefig(output, dpi=120, bbox_inches="tight")
    plt.close(fig)
    print(f"Saved: {output}  ({len(front)} solutions, {n_obj} objectives)")


if __name__ == "__main__":
    main()
