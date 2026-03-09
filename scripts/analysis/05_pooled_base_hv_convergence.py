"""Generate pooled base-HV convergence figures treating RE3D and RWA3D as one benchmark."""

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
from data_loader import available_runs, pooled_convergence_summary


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate pooled base-level HV convergence figures by treating "
            "RE3D and RWA3D as a single benchmark."
        )
    )
    parser.add_argument(
        "--results-root",
        type=Path,
        default=RESULTS_ROOT,
        help="Root directory containing the experiment folders.",
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


def pooled_run_count(front_type: str, budget: int, results_root: Path) -> int:
    """Count pooled runs across all families for one front/budget slice."""
    return sum(len(available_runs(family, front_type, budget, results_root)) for family in FAMILIES)


def normalize_progress(values: np.ndarray) -> np.ndarray:
    """Map a monotone HV curve to [0, 1] using its own start and end values."""
    start = float(values[0])
    end = float(values[-1])
    total_gain = end - start
    if abs(total_gain) < 1e-12:
        return np.zeros_like(values, dtype=float)
    return (values - start) / total_gain


def draw_pooled_hv_figure(budget: int, results_root: Path, normalized: bool) -> Path:
    """Create one pooled HV figure for one budget."""
    fig, axes = plt.subplots(1, 2, figsize=(13, 4.8), sharex=True)
    run_counts: list[int] = []

    for ax, front_type in zip(axes, FRONT_TYPES):
        stats = pooled_convergence_summary(FAMILIES, front_type, budget, results_root)
        run_counts.append(pooled_run_count(front_type, budget, results_root))

        evals = stats["Evaluation"].to_numpy()
        hv_median = (-stats["hv_median"]).to_numpy()
        hv_q25 = (-stats["hv_q75"]).to_numpy()
        hv_q75 = (-stats["hv_q25"]).to_numpy()

        color = FRONT_TYPE_COLORS[front_type]
        linestyle = FRONT_TYPE_LINESTYLES[front_type]
        label = FRONT_TYPE_LABELS[front_type]

        if normalized:
            hv_median = normalize_progress(hv_median)
            hv_q25 = normalize_progress(hv_q25)
            hv_q75 = normalize_progress(hv_q75)
            reached_95 = np.where(hv_median >= 0.95)[0]
            t95_eval = int(evals[reached_95[0]]) if len(reached_95) else None
            if t95_eval is not None:
                ax.axvline(t95_eval, color=color, linestyle=":", linewidth=1.4, alpha=0.9)
            ax.set_ylim(-0.05, 1.05)
            ax.set_title(
                f"{label}\nnormalized gain, T95={t95_eval if t95_eval is not None else 'NA'}",
                fontsize=11,
            )
            ax.set_ylabel("Normalized HV gain")
        else:
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
            ax.set_ylabel("HV of the pooled benchmark (higher is better)")

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
        ax.set_xlabel("Meta-evaluations")
        ax.legend()

    figure_label = "Normalized" if normalized else "Raw"
    fig.suptitle(
        f"{figure_label} pooled base-level HV convergence (budget {budget}, pooled runs: {min(run_counts)}-{max(run_counts)})",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])

    prefix = "base_hv_normalized_convergence_pooled" if normalized else "base_hv_convergence_pooled"
    return save_figure(fig, f"{prefix}_budget_{budget}.png")


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for budget in args.budgets:
        print(f"Generating pooled raw base HV curve for budget {budget}...")
        generated_paths.append(draw_pooled_hv_figure(budget, args.results_root, normalized=False))
        print(f"Generating pooled normalized base HV curve for budget {budget}...")
        generated_paths.append(draw_pooled_hv_figure(budget, args.results_root, normalized=True))

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
