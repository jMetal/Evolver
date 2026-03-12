"""Plot budget-wise convergence profiles and inferred convergence budgets."""

from __future__ import annotations

import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import (
    BUDGETS,
    FAMILIES,
    FRONT_TYPES,
    FRONT_TYPE_COLORS,
    FRONT_TYPE_LABELS,
    INDICATOR_SPECS,
    RESULTS_ROOT,
    TABLES_DIR,
    save_figure,
    setup_style,
)
from data_loader import final_best_summary

CONVERGENCE_THRESHOLD = 0.95


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Plot final performance against budget and mark the first budget "
            "reaching 95% of the total improvement."
        )
    )
    parser.add_argument(
        "--results-root",
        type=Path,
        default=RESULTS_ROOT,
        help="Optional root override containing all experiment folders.",
    )
    return parser.parse_args()


def build_budget_summary(results_root: Path | None) -> pd.DataFrame:
    """Collect final-best summaries for all family/front/budget slices."""
    rows: list[dict[str, float]] = []
    for family in FAMILIES:
        for front_type in FRONT_TYPES:
            for budget in BUDGETS:
                rows.append(final_best_summary(family, front_type, budget, results_root))
    return pd.DataFrame(rows).sort_values(["family", "front_type", "budget"]).reset_index(drop=True)


def infer_convergence_budget(values: np.ndarray, budgets: np.ndarray) -> float:
    """Return the first budget reaching 95% of the total observed improvement."""
    start = float(values[0])
    best = float(np.min(values))
    total_improvement = start - best
    if abs(total_improvement) < 1e-12:
        return float(budgets[0])

    normalized_gain = (start - values) / total_improvement
    reached = np.where(normalized_gain >= CONVERGENCE_THRESHOLD)[0]
    if len(reached) == 0:
        return float(budgets[-1])
    return float(budgets[reached[0]])


def add_convergence_marker(
    ax: plt.Axes,
    convergence_budget: float,
    color: str,
    label: str,
) -> None:
    """Draw a vertical marker for the inferred convergence budget."""
    ax.axvline(
        convergence_budget,
        color=color,
        linestyle=":",
        linewidth=1.4,
        alpha=0.9,
        label=f"{label} T95={int(convergence_budget)}",
    )


def plot_slice(df: pd.DataFrame, family: str, front_type: str) -> Path:
    """Create one separate figure for a family/front_type slice."""
    slice_df = (
        df[(df["family"] == family) & (df["front_type"] == front_type)]
        .sort_values("budget")
        .reset_index(drop=True)
    )
    budgets = slice_df["budget"].to_numpy(dtype=float)
    color = FRONT_TYPE_COLORS[front_type]
    label = FRONT_TYPE_LABELS[front_type]

    fig, axes = plt.subplots(1, 2, figsize=(13, 4.8), sharex=True)
    fig.suptitle(
        f"Budget convergence: {family} with {label.lower()}",
        fontsize=14,
        fontweight="bold",
    )

    for ax, (indicator_name, ylabel) in zip(axes, INDICATOR_SPECS):
        prefix = "EP" if indicator_name == "EP" else "HVMinus"
        median = slice_df[f"final_best_{prefix}_median"].to_numpy(dtype=float)
        q25 = slice_df[f"final_best_{prefix}_q25"].to_numpy(dtype=float)
        q75 = slice_df[f"final_best_{prefix}_q75"].to_numpy(dtype=float)
        convergence_budget = infer_convergence_budget(median, budgets)

        ax.plot(budgets, median, color=color, linewidth=2.0, marker="o", label=label)
        ax.fill_between(budgets, q25, q75, color=color, alpha=0.16)
        add_convergence_marker(ax, convergence_budget, color, indicator_name)

        ax.set_xlim(0, 7000)
        ax.set_xticks([0, 1000, 3000, 5000, 7000])
        ax.set_xlabel("Base-level budget")
        ax.set_ylabel(ylabel)
        ax.set_title(indicator_name)
        ax.legend()

    fig.tight_layout(rect=[0, 0, 1, 0.94])
    return save_figure(fig, f"budget_convergence_{family}_{front_type}.png")


def build_convergence_budget_table(summary_df: pd.DataFrame) -> pd.DataFrame:
    """Infer and store the convergence budget for EP and HVMinus."""
    rows: list[dict[str, float]] = []
    for family in FAMILIES:
        for front_type in FRONT_TYPES:
            slice_df = (
                summary_df[
                    (summary_df["family"] == family) & (summary_df["front_type"] == front_type)
                ]
                .sort_values("budget")
                .reset_index(drop=True)
            )
            budgets = slice_df["budget"].to_numpy(dtype=float)
            ep_values = slice_df["final_best_EP_median"].to_numpy(dtype=float)
            hv_values = slice_df["final_best_HVMinus_median"].to_numpy(dtype=float)
            rows.append(
                {
                    "family": family,
                    "front_type": front_type,
                    "budget_T95_EP": infer_convergence_budget(ep_values, budgets),
                    "budget_T95_HVMinus": infer_convergence_budget(hv_values, budgets),
                    "final_best_budget_EP": float(budgets[int(np.argmin(ep_values))]),
                    "final_best_budget_HVMinus": float(budgets[int(np.argmin(hv_values))]),
                }
            )
    return pd.DataFrame(rows)


def main() -> None:
    args = parse_args()
    setup_style()

    summary_df = build_budget_summary(args.results_root)
    summary_path = TABLES_DIR / "budget_convergence_summary.csv"
    summary_df.to_csv(summary_path, index=False)

    convergence_budget_df = build_convergence_budget_table(summary_df)
    convergence_path = TABLES_DIR / "budget_convergence_t95.csv"
    convergence_budget_df.to_csv(convergence_path, index=False)

    generated_paths: list[Path] = []
    for family in FAMILIES:
        for front_type in FRONT_TYPES:
            print(f"Generating {family} / {front_type}...")
            generated_paths.append(plot_slice(summary_df, family, front_type))

    print(f"Saved summary: {summary_path}")
    print(f"Saved convergence markers: {convergence_path}")
    print("Generated figures:")
    for path in generated_paths:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
