"""Generate one convergence figure per family and budget."""

from __future__ import annotations

import argparse
from pathlib import Path

import matplotlib.pyplot as plt

from config import (
    BUDGETS,
    FAMILIES,
    FRONT_TYPES,
    FRONT_TYPE_COLORS,
    FRONT_TYPE_LABELS,
    FRONT_TYPE_LINESTYLES,
    INDICATOR_SPECS,
    RESULTS_ROOT,
    save_figure,
    setup_style,
)
from data_loader import available_runs, convergence_summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate convergence figures for RE3D and RWA3D budgets, "
            "comparing referenceFronts vs estimatedReferenceFronts."
        )
    )
    parser.add_argument(
        "--results-root",
        type=Path,
        default=RESULTS_ROOT,
        help="Root directory containing the experiment folders.",
    )
    parser.add_argument(
        "--families",
        nargs="+",
        default=FAMILIES,
        choices=FAMILIES,
        help="Problem families to plot.",
    )
    parser.add_argument(
        "--budgets",
        nargs="+",
        type=int,
        default=BUDGETS,
        choices=BUDGETS,
        help="Budgets to plot.",
    )
    return parser.parse_args()


def draw_family_budget_figure(family: str, budget: int, results_root: Path) -> Path:
    """Create a figure with EP and HVMinus for one family and one budget."""
    fig, axes = plt.subplots(1, 2, figsize=(13, 4.8), sharex=True)

    run_counts: list[int] = []
    for front_type in FRONT_TYPES:
        stats = convergence_summary(family, front_type, budget, results_root)
        run_counts.append(len(available_runs(family, front_type, budget, results_root)))
        evals = stats["Evaluation"].to_numpy()
        color = FRONT_TYPE_COLORS[front_type]
        linestyle = FRONT_TYPE_LINESTYLES[front_type]
        label = FRONT_TYPE_LABELS[front_type]

        for axis, (indicator_name, ylabel) in zip(axes, INDICATOR_SPECS):
            prefix = "ep" if indicator_name == "EP" else "hv"
            median = stats[f"{prefix}_median"].to_numpy()
            q25 = stats[f"{prefix}_q25"].to_numpy()
            q75 = stats[f"{prefix}_q75"].to_numpy()

            axis.plot(
                evals,
                median,
                color=color,
                linestyle=linestyle,
                linewidth=1.8,
                label=label,
            )
            axis.fill_between(evals, q25, q75, color=color, alpha=0.16)
            axis.set_title(indicator_name)
            axis.set_xlabel("Meta-evaluations")
            axis.set_ylabel(ylabel)
            axis.legend()

    fig.suptitle(
        f"Convergence analysis: {family}, budget {budget} "
        f"(runs: {min(run_counts)}-{max(run_counts)})",
        fontsize=14,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.95])
    return save_figure(fig, f"convergence_{family}_budget_{budget}.png")


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for family in args.families:
        for budget in args.budgets:
            print(f"Generating {family} budget {budget}...")
            generated_paths.append(
                draw_family_budget_figure(family, budget, args.results_root)
            )

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
