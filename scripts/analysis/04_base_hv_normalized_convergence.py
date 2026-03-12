"""Generate normalized base-HV convergence curves without replacing raw-HV figures."""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
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
            "Generate normalized base-level HV convergence figures over meta-evaluations."
        )
    )
    parser.add_argument(
        "--results-root",
        type=Path,
        default=RESULTS_ROOT,
        help="Optional root override containing all experiment folders.",
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


def normalize_progress(values: np.ndarray) -> np.ndarray:
    """Map a monotone HV curve to [0, 1] using its own start and end values."""
    start = float(values[0])
    end = float(values[-1])
    total_gain = end - start
    if abs(total_gain) < 1e-12:
        return np.zeros_like(values, dtype=float)
    return (values - start) / total_gain


def draw_normalized_hv_figure(
    family: str, budget: int, results_root: Path | None
) -> Path:
    """Create one normalized HV convergence figure for a family and budget."""
    fig, axes = plt.subplots(1, 2, figsize=(13, 4.8), sharex=True, sharey=True)
    run_counts: list[int] = []

    for ax, front_type in zip(axes, FRONT_TYPES):
        stats = convergence_summary(family, front_type, budget, results_root)
        run_counts.append(len(available_runs(family, front_type, budget, results_root)))

        evals = stats["Evaluation"].to_numpy()
        hv_median = (-stats["hv_median"]).to_numpy()
        hv_q25 = (-stats["hv_q75"]).to_numpy()
        hv_q75 = (-stats["hv_q25"]).to_numpy()

        hv_norm = normalize_progress(hv_median)
        hv_q25_norm = normalize_progress(hv_q25)
        hv_q75_norm = normalize_progress(hv_q75)

        color = FRONT_TYPE_COLORS[front_type]
        linestyle = FRONT_TYPE_LINESTYLES[front_type]
        label = FRONT_TYPE_LABELS[front_type]

        ax.plot(
            evals,
            hv_norm,
            color=color,
            linestyle=linestyle,
            linewidth=2.0,
            label=label,
        )
        ax.fill_between(evals, hv_q25_norm, hv_q75_norm, color=color, alpha=0.16)
        ax.scatter([evals[0], evals[-1]], [hv_norm[0], hv_norm[-1]], color=color, s=22, zorder=3)

        reached_95 = np.where(hv_norm >= 0.95)[0]
        if len(reached_95):
            t95_eval = int(evals[reached_95[0]])
            ax.axvline(t95_eval, color=color, linestyle=":", linewidth=1.4, alpha=0.9)
        else:
            t95_eval = None

        ax.set_ylim(-0.05, 1.05)
        ax.set_title(
            f"{label}\nnormalized gain, T95={t95_eval if t95_eval is not None else 'NA'}",
            fontsize=11,
        )
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("Normalized HV gain")
        ax.legend()

    fig.suptitle(
        f"Normalized base-level HV convergence ({family}, budget {budget}, runs: {min(run_counts)}-{max(run_counts)})",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(fig, f"base_hv_normalized_convergence_{family}_budget_{budget}.png")


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for family in args.families:
        for budget in args.budgets:
            print(f"Generating normalized base HV curve for {family} budget {budget}...")
            generated_paths.append(draw_normalized_hv_figure(family, budget, args.results_root))

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
