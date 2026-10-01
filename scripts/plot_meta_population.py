#!/usr/bin/env python3
"""
Plot the population of the meta-optimizer at several checkpoints of a training run, in the space
of two meta-objectives, with its non-dominated configurations highlighted.

It reads POPULATION_INDICATORS.csv, which a training run writes when it is asked to write the
whole population (``writePopulation: true`` in the request), and INDICATORS.csv, which has the
non-dominated configurations of each checkpoint. It is the static counterpart of the live plot of
a training run (``frontPlotFrequency``), and also shows the final checkpoint.

Usage:
    python scripts/plot_meta_population.py <training output directory> \\
        [--evaluations 100,1000,2000] [--x EP] [--y NHV] [--output meta_population.png]

--evaluations defaults to the first, the middle and the last checkpoint. Each panel has its own
axes, since the population usually spans very different ranges early and late in the training.
Checkpoints are chosen by meta-evaluation; when VAR_CONF.txt records the computing time of each
checkpoint (Evolver 2.2 or later), each panel shows it too, in the unit of the whole run.
"""

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402

from training_time import checkpoint_minutes, format_time  # noqa: E402

POPULATION_COLOR = "#1f77b4"
FRONT_COLOR = "#d62728"


def checkpoints(csv_file: Path) -> dict[int, pd.DataFrame]:
    """The rows of each checkpoint. A checkpoint written twice (e.g. the final one) keeps the
    last block: a new block starts wherever SolutionId goes back to 0."""
    data = pd.read_csv(csv_file)
    block = (data["SolutionId"] == 0).cumsum()
    result = {}
    for _, rows in data.groupby(block, sort=True):
        result[int(rows["Evaluation"].iloc[0])] = rows
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description="Population of the meta-optimizer over time")
    parser.add_argument("training", type=Path, help="output directory of a training run")
    parser.add_argument("--evaluations", help="comma-separated checkpoints to plot")
    parser.add_argument("--x", default="EP", help="meta-objective on the x axis (default: EP)")
    parser.add_argument("--y", default="NHV", help="meta-objective on the y axis (default: NHV)")
    parser.add_argument("--output", type=Path, default=Path("meta_population.png"))
    args = parser.parse_args()

    population_file = args.training / "POPULATION_INDICATORS.csv"
    if not population_file.exists():
        sys.exit(
            f"{population_file} not found: run the training with writePopulation: true"
        )
    population = checkpoints(population_file)
    front = checkpoints(args.training / "INDICATORS.csv")

    times = checkpoint_minutes(args.training / "VAR_CONF.txt")
    longest = max(times.values()) if times else 0.0
    available = sorted(population)
    if args.evaluations:
        evaluations = [int(e) for e in args.evaluations.split(",")]
        missing = [e for e in evaluations if e not in population]
        if missing:
            sys.exit(f"No checkpoint at {missing}; available: {available}")
    else:
        evaluations = sorted({available[0], available[len(available) // 2], available[-1]})

    fig, axes = plt.subplots(1, len(evaluations), figsize=(4.2 * len(evaluations), 3.8))
    if len(evaluations) == 1:
        axes = [axes]
    for ax, evaluation in zip(axes, evaluations):
        rows = population[evaluation]
        ax.scatter(rows[args.x], rows[args.y], s=18, c=POPULATION_COLOR, label="population")
        if evaluation in front:
            non_dominated = front[evaluation]
            ax.scatter(non_dominated[args.x], non_dominated[args.y], s=40, facecolors="none",
                       edgecolors=FRONT_COLOR, linewidths=1.5, label="non-dominated")
        title = f"After {evaluation} evaluations"
        if evaluation in times:
            title += f" ({format_time(times[evaluation], longest)})"
        ax.set_title(title, fontsize=10)
        ax.set_xlabel(args.x)
        ax.set_ylabel(args.y)
        ax.grid(True, linestyle=":", alpha=0.6)
    axes[0].legend(fontsize=8)
    fig.tight_layout()
    fig.savefig(args.output, dpi=150)
    print(f"Written {args.output}")


if __name__ == "__main__":
    main()
