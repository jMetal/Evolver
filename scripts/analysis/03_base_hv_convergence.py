"""Generate readable HV convergence curves from the stored HVMinus traces."""

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
    RESULTS_ROOT,
    save_figure,
    setup_style,
)
from data_loader import available_runs, convergence_summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate base-level HV convergence figures over meta-evaluations, "
            "using HV = -HVMinus."
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


def draw_hv_figure(family: str, budget: int, results_root: Path) -> Path:
    """Create one HV convergence figure with separate panels per front type."""
    fig, axes = plt.subplots(1, 2, figsize=(13, 4.8), sharex=True)
    run_counts: list[int] = []

    for ax, front_type in zip(axes, FRONT_TYPES):
        stats = convergence_summary(family, front_type, budget, results_root)
        run_counts.append(len(available_runs(family, front_type, budget, results_root)))

        evals = stats["Evaluation"].to_numpy()
        hv_median = (-stats["hv_median"]).to_numpy()
        hv_q25 = (-stats["hv_q75"]).to_numpy()
        hv_q75 = (-stats["hv_q25"]).to_numpy()

        color = FRONT_TYPE_COLORS[front_type]
        linestyle = FRONT_TYPE_LINESTYLES[front_type]
        label = FRONT_TYPE_LABELS[front_type]

        ax.plot(
            evals,
            hv_median,
            color=color,
            linestyle=linestyle,
            linewidth=1.9,
            label=label,
        )
        ax.fill_between(evals, hv_q25, hv_q75, color=color, alpha=0.16)
        ax.scatter([evals[0], evals[-1]], [hv_median[0], hv_median[-1]], color=color, s=22, zorder=3)

        start = float(hv_median[0])
        end = float(hv_median[-1])
        delta = end - start
        span = max(float(hv_q75.max() - hv_q25.min()), abs(delta), 1e-6)
        margin = span * 0.18
        ax.set_ylim(float(hv_q25.min() - margin), float(hv_q75.max() + margin))

        ax.set_title(
            f"{label}\nstart={start:.6f}  end={end:.6f}  delta={delta:.6f}",
            fontsize=11,
        )
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("HV of the base algorithm (higher is better)")
        ax.legend()

    fig.suptitle(
        f"Base-level HV convergence ({family}, budget {budget}, runs: {min(run_counts)}-{max(run_counts)})",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(fig, f"base_hv_convergence_{family}_budget_{budget}.png")


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for family in args.families:
        for budget in args.budgets:
            print(f"Generating base HV curve for {family} budget {budget}...")
            generated_paths.append(draw_hv_figure(family, budget, args.results_root))

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
