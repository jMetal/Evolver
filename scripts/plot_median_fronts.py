#!/usr/bin/env python3
"""
Plot the representative fronts of a jMetal validation study: for each problem and algorithm, the
front of the run with the median value of an indicator (HV by default), over the reference front.

A jMetal experiment (ExperimentBuilder) writes, under <experiment>/data/<algorithm>/<problem>/,
one FUN<i>.csv per run and one file per indicator (e.g. HV) with the value of run i on line i+1.
The figure is a grid with one row per problem and one column per algorithm; each panel is titled
with the median value. Bi-objective fronts are drawn in 2D and three-objective ones in 3D.

Usage:
    python scripts/plot_median_fronts.py <experiment directory> \\
        --problems DTLZ1,DTLZ3,DTLZ7 --algorithms NSGAII,NSGAIII,SMSEMOA,AGEMOEA,NSGAIIDTLZ \\
        --reference-fronts resources/referenceFronts --reference-suffix .3D.csv \\
        [--indicator HV] [--output median_fronts.png]

The reference front of problem P is <reference-fronts>/<P><reference-suffix>, since reference
fronts follow no single naming convention (DTLZ1.3D.csv, RE31.csv).
"""

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402
from mpl_toolkits.mplot3d import Axes3D  # noqa: F401,E402  (registers the 3d projection)

FRONT_COLOR = "#1f77b4"
REF_COLOR = "#bbbbbb"


def median_run(indicator_file: Path) -> tuple[int, float]:
    """Index of the run with the median indicator value (the lower one for an even count)."""
    values = [float(line) for line in indicator_file.read_text().split() if line.strip()]
    order = np.argsort(values)
    index = int(order[(len(values) - 1) // 2])
    return index, values[index]


def draw(ax, front, reference, three_d):
    if three_d:
        ax.scatter(*reference[:, :3].T, s=3, c=REF_COLOR, edgecolors="none", depthshade=False)
        ax.scatter(*front[:, :3].T, s=8, c=FRONT_COLOR, edgecolors="none", depthshade=False)
        ax.view_init(elev=30, azim=45)
        ax.tick_params(labelsize=6, pad=0)
    else:
        ax.scatter(reference[:, 0], reference[:, 1], s=3, c=REF_COLOR, edgecolors="none")
        ax.scatter(front[:, 0], front[:, 1], s=8, c=FRONT_COLOR, edgecolors="none")
        ax.tick_params(labelsize=6)


def main() -> None:
    parser = argparse.ArgumentParser(description="Median-indicator fronts of a jMetal study")
    parser.add_argument("experiment", type=Path, help="experiment directory (with data/)")
    parser.add_argument("--problems", required=True, help="comma-separated problems (rows)")
    parser.add_argument("--algorithms", required=True, help="comma-separated algorithms (columns)")
    parser.add_argument("--reference-fronts", type=Path, required=True)
    parser.add_argument("--reference-suffix", default=".csv")
    parser.add_argument("--indicator", default="HV")
    parser.add_argument("--output", type=Path, default=Path("median_fronts.png"))
    args = parser.parse_args()

    problems = args.problems.split(",")
    algorithms = args.algorithms.split(",")
    data = args.experiment / "data"
    if not data.is_dir():
        sys.exit(f"No data directory in {args.experiment}")

    fig = plt.figure(figsize=(3.2 * len(algorithms), 3.0 * len(problems)))
    for row, problem in enumerate(problems):
        reference = pd.read_csv(
            args.reference_fronts / f"{problem}{args.reference_suffix}", header=None
        ).values
        three_d = reference.shape[1] >= 3
        for column, algorithm in enumerate(algorithms):
            directory = data / algorithm / problem
            run, value = median_run(directory / args.indicator)
            front = pd.read_csv(directory / f"FUN{run}.csv", header=None).values
            ax = fig.add_subplot(
                len(problems),
                len(algorithms),
                row * len(algorithms) + column + 1,
                projection="3d" if three_d else None,
            )
            draw(ax, front, reference, three_d)
            ax.set_title(f"{algorithm}\n{args.indicator} = {value:.4f}", fontsize=8)
            if column == 0:
                if three_d:
                    ax.text2D(-0.15, 0.5, problem, transform=ax.transAxes, rotation=90,
                              va="center", fontsize=10, fontweight="bold")
                else:
                    ax.set_ylabel(problem, fontsize=10, fontweight="bold")
    fig.tight_layout()
    fig.savefig(args.output, dpi=150)
    print(f"Written {args.output}")


if __name__ == "__main__":
    main()
