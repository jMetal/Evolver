"""Generate comparative absolute base-HV curves across budgets."""

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
            "Generate comparative absolute base-level HV figures across budgets "
            "without replacing the existing normalized plots."
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
    parser.add_argument(
        "--tag",
        type=str,
        default="",
        help="Optional suffix to append to output file names.",
    )
    return parser.parse_args()


def add_budget_curve(
    ax: plt.Axes,
    evals: np.ndarray,
    hv_median: np.ndarray,
    hv_q25: np.ndarray,
    hv_q75: np.ndarray,
    budget: int,
) -> tuple[float, float]:
    """Plot one absolute HV curve and return its vertical span."""
    color = BUDGET_COLORS[budget]
    start = float(hv_median[0])
    end = float(hv_median[-1])
    delta = end - start
    label = f"budget {budget} (start={start:.4f}, end={end:.4f}, delta={delta:.4f})"

    ax.plot(evals, hv_median, color=color, linewidth=2.1, label=label)
    ax.fill_between(evals, hv_q25, hv_q75, color=color, alpha=0.08)
    ax.scatter([evals[0], evals[-1]], [hv_median[0], hv_median[-1]], color=color, s=26, zorder=3)
    return float(hv_q25.min()), float(hv_q75.max())


def file_suffix(tag: str) -> str:
    """Return a normalized suffix for output file names."""
    clean = tag.strip().replace(" ", "_")
    return f"_{clean}" if clean else ""


def draw_family_figure(
    family: str, budgets: list[int], results_root: Path | None, tag: str
) -> Path:
    """Create one comparative absolute HV figure for a single family."""
    fig, axes = plt.subplots(1, 2, figsize=(14.5, 4.8), sharex=True)

    for ax, front_type in zip(axes, FRONT_TYPES):
        lower_bounds: list[float] = []
        upper_bounds: list[float] = []

        for budget in budgets:
            stats = convergence_summary(family, front_type, budget, results_root)
            evals = stats["Evaluation"].to_numpy()
            hv_median = (-stats["hv_median"]).to_numpy()
            hv_q25 = (-stats["hv_q75"]).to_numpy()
            hv_q75 = (-stats["hv_q25"]).to_numpy()
            lower, upper = add_budget_curve(ax, evals, hv_median, hv_q25, hv_q75, budget)
            lower_bounds.append(lower)
            upper_bounds.append(upper)

        span = max(upper_bounds) - min(lower_bounds)
        margin = max(span * 0.12, 1e-4)
        ax.set_ylim(min(lower_bounds) - margin, max(upper_bounds) + margin)
        ax.set_title(FRONT_TYPE_LABELS[front_type], fontsize=11)
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("Absolute HV")
        ax.legend(loc="lower right")

    fig.suptitle(
        f"Comparative absolute base-level HV across budgets ({family})",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(
        fig,
        str(
            Path("base_hv_budget_comparison")
            / f"base_hv_absolute_budget_comparison_{family}{file_suffix(tag)}.png"
        ),
    )


def draw_pooled_figure(
    budgets: list[int], results_root: Path | None, tag: str
) -> Path:
    """Create one comparative absolute HV figure pooling all families."""
    fig, axes = plt.subplots(1, 2, figsize=(14.5, 4.8), sharex=True)

    for ax, front_type in zip(axes, FRONT_TYPES):
        lower_bounds: list[float] = []
        upper_bounds: list[float] = []

        for budget in budgets:
            stats = pooled_convergence_summary(FAMILIES, front_type, budget, results_root)
            evals = stats["Evaluation"].to_numpy()
            hv_median = (-stats["hv_median"]).to_numpy()
            hv_q25 = (-stats["hv_q75"]).to_numpy()
            hv_q75 = (-stats["hv_q25"]).to_numpy()
            lower, upper = add_budget_curve(ax, evals, hv_median, hv_q25, hv_q75, budget)
            lower_bounds.append(lower)
            upper_bounds.append(upper)

        span = max(upper_bounds) - min(lower_bounds)
        margin = max(span * 0.12, 1e-4)
        ax.set_ylim(min(lower_bounds) - margin, max(upper_bounds) + margin)
        ax.set_title(FRONT_TYPE_LABELS[front_type], fontsize=11)
        ax.set_xlabel("Meta-evaluations")
        ax.set_ylabel("Absolute HV")
        ax.legend(loc="lower right")

    fig.suptitle(
        "Comparative absolute base-level HV across budgets (RE3D + RWA3D pooled)",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(
        fig,
        str(
            Path("base_hv_budget_comparison")
            / f"base_hv_absolute_budget_comparison_pooled{file_suffix(tag)}.png"
        ),
    )


def main() -> None:
    args = parse_args()
    setup_style()

    generated_paths: list[Path] = []
    for family in args.families:
        print(f"Generating comparative absolute base HV figure for {family}...")
        generated_paths.append(draw_family_figure(family, args.budgets, args.results_root, args.tag))

    print("Generating comparative absolute base HV figure for pooled benchmark...")
    generated_paths.append(draw_pooled_figure(args.budgets, args.results_root, args.tag))

    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
