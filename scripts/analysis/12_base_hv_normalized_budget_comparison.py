"""Generate comparative normalized base-HV curves across budgets."""

from __future__ import annotations

import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

from config import BUDGETS, FAMILIES, FRONT_TYPES, FRONT_TYPE_LABELS, RESULTS_ROOT, save_figure, setup_style
from data_loader import convergence_summary, pooled_convergence_summary

BUDGET_COLORS = {
    1000: "#1b9e77",
    3000: "#d95f02",
    5000: "#7570b3",
    7000: "#e7298a",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Generate comparative normalized base-level HV figures across budgets "
            "without replacing the existing convergence plots."
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
        help="Budgets to compare.",
    )
    parser.add_argument(
        "--families",
        nargs="+",
        default=FAMILIES,
        choices=FAMILIES,
        help="Families to compare individually.",
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


def add_budget_curve(ax: plt.Axes, evals: np.ndarray, hv_median: np.ndarray, hv_q25: np.ndarray, hv_q75: np.ndarray, budget: int) -> None:
    """Plot one normalized HV curve with a T95 marker."""
    hv_norm = normalize_progress(hv_median)
    hv_q25_norm = normalize_progress(hv_q25)
    hv_q75_norm = normalize_progress(hv_q75)

    color = BUDGET_COLORS[budget]
    reached_95 = np.where(hv_norm >= 0.95)[0]
    t95_eval = int(evals[reached_95[0]]) if len(reached_95) else None
    label = f"budget {budget} (T95={t95_eval if t95_eval is not None else 'NA'})"

    ax.plot(evals, hv_norm, color=color, linewidth=2.1, label=label)
    ax.fill_between(evals, hv_q25_norm, hv_q75_norm, color=color, alpha=0.08)
    if t95_eval is not None:
        marker_y = float(hv_norm[reached_95[0]])
        ax.scatter([t95_eval], [marker_y], color=color, s=28, zorder=3)


def draw_family_figure(
    family: str, budgets: list[int], results_root: Path | None
) -> Path:
    """Create one comparative normalized HV figure for a single family."""
    fig, axes = plt.subplots(1, 2, figsize=(13.8, 4.8), sharex=True, sharey=True)

    for ax, front_type in zip(axes, FRONT_TYPES):
        for budget in budgets:
            stats = convergence_summary(family, front_type, budget, results_root)
            evals = stats["Evaluation"].to_numpy()
            hv_median = (-stats["hv_median"]).to_numpy()
            hv_q25 = (-stats["hv_q75"]).to_numpy()
            hv_q75 = (-stats["hv_q25"]).to_numpy()
            add_budget_curve(ax, evals, hv_median, hv_q25, hv_q75, budget)

        ax.set_title(FRONT_TYPE_LABELS[front_type], fontsize=11)
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("Normalized HV gain")
        ax.set_ylim(-0.05, 1.05)
        ax.legend(loc="lower right")

    fig.suptitle(
        f"Comparative normalized base-level HV across budgets ({family})",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(
        fig,
        str(Path("base_hv_budget_comparison") / f"base_hv_normalized_budget_comparison_{family}.png"),
    )


def draw_pooled_figure(budgets: list[int], results_root: Path | None) -> Path:
    """Create one comparative normalized HV figure pooling all families."""
    fig, axes = plt.subplots(1, 2, figsize=(13.8, 4.8), sharex=True, sharey=True)

    for ax, front_type in zip(axes, FRONT_TYPES):
        for budget in budgets:
            stats = pooled_convergence_summary(FAMILIES, front_type, budget, results_root)
            evals = stats["Evaluation"].to_numpy()
            hv_median = (-stats["hv_median"]).to_numpy()
            hv_q25 = (-stats["hv_q75"]).to_numpy()
            hv_q75 = (-stats["hv_q25"]).to_numpy()
            add_budget_curve(ax, evals, hv_median, hv_q25, hv_q75, budget)

        ax.set_title(FRONT_TYPE_LABELS[front_type], fontsize=11)
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("Normalized HV gain")
        ax.set_ylim(-0.05, 1.05)
        ax.legend(loc="lower right")

    fig.suptitle(
        "Comparative normalized base-level HV across budgets (RE3D + RWA3D pooled)",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(
        fig,
        str(Path("base_hv_budget_comparison") / "base_hv_normalized_budget_comparison_pooled.png"),
    )


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for family in args.families:
        print(f"Generating comparative normalized base HV figure for {family}...")
        generated_paths.append(draw_family_figure(family, args.budgets, args.results_root))

    print("Generating comparative normalized base HV figure for pooled benchmark...")
    generated_paths.append(draw_pooled_figure(args.budgets, args.results_root))

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
