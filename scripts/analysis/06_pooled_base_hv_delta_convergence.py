"""Generate pooled delta-HV convergence figures for the combined RE3D+RWA3D benchmark."""

from __future__ import annotations

import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

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
from data_loader import available_runs, pooled_convergence_summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate pooled delta-HV convergence figures for the combined RE3D+RWA3D benchmark."
        )
    )
    parser.add_argument(
        "--results-root",
        type=Path,
        default=RESULTS_ROOT,
        help="Optional root override containing all experiment folders.",
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


def pooled_run_count(front_type: str, budget: int, results_root: Path | None) -> int:
    """Count pooled runs across all families for one front/budget slice."""
    return sum(len(available_runs(family, front_type, budget, results_root)) for family in FAMILIES)


def as_delta(values: np.ndarray) -> np.ndarray:
    """Convert a series to change-from-start coordinates."""
    return values - float(values[0])


def draw_delta_hv_figure(budget: int, results_root: Path | None) -> Path:
    """Create one pooled delta-HV figure for one budget."""
    fig, axes = plt.subplots(1, 2, figsize=(13, 4.8), sharex=True)
    run_counts: list[int] = []

    for ax, front_type in zip(axes, FRONT_TYPES):
        stats = pooled_convergence_summary(FAMILIES, front_type, budget, results_root)
        run_counts.append(pooled_run_count(front_type, budget, results_root))

        evals = stats["Evaluation"].to_numpy()
        hv_median = as_delta((-stats["hv_median"]).to_numpy())
        hv_q25 = as_delta((-stats["hv_q75"]).to_numpy())
        hv_q75 = as_delta((-stats["hv_q25"]).to_numpy())

        color = FRONT_TYPE_COLORS[front_type]
        linestyle = FRONT_TYPE_LINESTYLES[front_type]
        label = FRONT_TYPE_LABELS[front_type]

        ax.plot(
            evals,
            hv_median,
            color=color,
            linestyle=linestyle,
            linewidth=2.0,
            label=label,
        )
        ax.fill_between(evals, hv_q25, hv_q75, color=color, alpha=0.16)
        ax.scatter([evals[0], evals[-1]], [hv_median[0], hv_median[-1]], color=color, s=22, zorder=3)
        ax.axhline(0.0, color="#666666", linestyle=":", linewidth=1.0, alpha=0.8)

        end_delta = float(hv_median[-1])
        span = max(float(hv_q75.max() - hv_q25.min()), abs(end_delta), 1e-8)
        margin = span * 0.18
        ax.set_ylim(float(hv_q25.min() - margin), float(hv_q75.max() + margin))
        ax.ticklabel_format(axis="y", style="sci", scilimits=(-3, 3))
        ax.set_title(
            f"{label}\nend delta={end_delta:.6e}",
            fontsize=11,
        )
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("Delta HV from first checkpoint")
        ax.legend()

    fig.suptitle(
        f"Pooled delta-HV convergence (budget {budget}, pooled runs: {min(run_counts)}-{max(run_counts)})",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(fig, f"base_hv_delta_convergence_pooled_budget_{budget}.png")


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for budget in args.budgets:
        print(f"Generating pooled delta HV curve for budget {budget}...")
        generated_paths.append(draw_delta_hv_figure(budget, args.results_root))

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
