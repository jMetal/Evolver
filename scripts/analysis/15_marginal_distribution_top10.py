"""Compare parameter distributions of top-10% configurations vs the rest."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

from config import (
    BUDGETS,
    FIGURES_DIR,
    FRONT_TYPE_LABELS,
    FRONT_TYPES,
    TABLES_DIR,
    TOP_PARAMETERS,
    save_figure,
    setup_style,
)
from data_loader import (
    get_final_configs,
    load_all_runs_with_config,
)

TOP_QUANTILE = 0.10
SLICES = [
    (family, front_type)
    for front_type in FRONT_TYPES
    for family in ["RE3D", "RWA3D"]
]
TARGETS = [
    ("EP", "EP"),
    ("HVMinus", "HV$^-$"),
]
FIGURE_OUTPUT_ROOT = FIGURES_DIR / "marginal_distribution"
TABLE_OUTPUT_ROOT = TABLES_DIR / "marginal_distribution"
FIGURE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
TABLE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def split_top_rest(
    df: pd.DataFrame, target: str, quantile: float = TOP_QUANTILE
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Split into top-quantile (lowest values) and the rest."""
    threshold = df[target].quantile(quantile)
    top = df[df[target] <= threshold]
    rest = df[df[target] > threshold]
    return top, rest


def compute_stats(
    top: pd.DataFrame, rest: pd.DataFrame, param: str
) -> dict[str, object]:
    """Compute comparison statistics for one parameter."""
    top_vals = top[param].dropna()
    rest_vals = rest[param].dropna()

    result: dict[str, object] = {
        "parameter": param,
        "top_n": len(top_vals),
        "rest_n": len(rest_vals),
        "top_median": float(top_vals.median()) if len(top_vals) else np.nan,
        "top_q25": float(top_vals.quantile(0.25)) if len(top_vals) else np.nan,
        "top_q75": float(top_vals.quantile(0.75)) if len(top_vals) else np.nan,
        "rest_median": float(rest_vals.median()) if len(rest_vals) else np.nan,
        "rest_q25": float(rest_vals.quantile(0.25)) if len(rest_vals) else np.nan,
        "rest_q75": float(rest_vals.quantile(0.75)) if len(rest_vals) else np.nan,
    }

    if len(top_vals) >= 2 and len(rest_vals) >= 2:
        u_stat, p_value = stats.mannwhitneyu(top_vals, rest_vals, alternative="two-sided")
        result["u_stat"] = float(u_stat)
        result["p_value"] = float(p_value)
        result["significant"] = p_value < 0.05
    else:
        result["u_stat"] = np.nan
        result["p_value"] = np.nan
        result["significant"] = False

    return result


def plot_kde(
    top: pd.DataFrame,
    rest: pd.DataFrame,
    params: list[str],
    scope: str,
    front_type: str,
    budget: int,
    target: str,
    target_label: str,
) -> Path:
    """Create 2x2 KDE comparison for key parameters."""
    fig, axes = plt.subplots(2, 2, figsize=(12, 10))
    fig.suptitle(
        f"Top 10% vs Rest | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | {target_label}",
        fontsize=13,
        fontweight="bold",
    )

    for ax, param in zip(axes.flat, params, strict=False):
        top_vals = top[param].dropna()
        rest_vals = rest[param].dropna()

        if len(rest_vals) >= 2:
            rest_vals.plot.kde(ax=ax, color="grey", alpha=0.6, label="Rest (90%)", linewidth=2)
            ax.fill_between(
                ax.lines[-1].get_xdata(),
                ax.lines[-1].get_ydata(),
                alpha=0.15,
                color="grey",
            )
        if len(top_vals) >= 2:
            top_vals.plot.kde(ax=ax, color="#1b9e77", alpha=0.9, label="Top 10%", linewidth=2)
            ax.fill_between(
                ax.lines[-1].get_xdata(),
                ax.lines[-1].get_ydata(),
                alpha=0.2,
                color="#1b9e77",
            )

        if len(top_vals):
            ax.axvline(top_vals.median(), color="#1b9e77", linestyle="--", alpha=0.8, linewidth=1.5)
        if len(rest_vals):
            ax.axvline(rest_vals.median(), color="grey", linestyle="--", alpha=0.8, linewidth=1.5)

        ax.set_title(param, fontsize=10)
        ax.set_ylabel("Density")
        ax.legend(fontsize=8)

    for ax in axes.flat[len(params) :]:
        ax.set_visible(False)

    fig.tight_layout()
    filename = f"marginal_kde_{scope}_{front_type}_{budget}_{target}.png"
    return save_figure(fig, str(Path("marginal_distribution") / f"budget_{budget}" / filename))


def plot_violin(
    top: pd.DataFrame,
    rest: pd.DataFrame,
    params: list[str],
    scope: str,
    front_type: str,
    budget: int,
    target: str,
    target_label: str,
) -> Path:
    """Create violin plots comparing top-10% vs rest."""
    fig, axes = plt.subplots(1, len(params), figsize=(4 * len(params), 6))
    if len(params) == 1:
        axes = [axes]
    fig.suptitle(
        f"Violin: Top 10% vs Rest | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | {target_label}",
        fontsize=13,
        fontweight="bold",
    )

    for ax, param in zip(axes, params, strict=False):
        top_vals = top[param].dropna().values
        rest_vals = rest[param].dropna().values

        data_to_plot = []
        labels = []
        colors = []
        if len(rest_vals) >= 2:
            data_to_plot.append(rest_vals)
            labels.append("Rest")
            colors.append("grey")
        if len(top_vals) >= 2:
            data_to_plot.append(top_vals)
            labels.append("Top 10%")
            colors.append("#1b9e77")

        if data_to_plot:
            parts = ax.violinplot(data_to_plot, showmeans=True, showmedians=True)
            for i, pc in enumerate(parts["bodies"]):
                pc.set_facecolor(colors[i])
                pc.set_alpha(0.6)
            ax.set_xticks(range(1, len(labels) + 1))
            ax.set_xticklabels(labels, fontsize=9)

        ax.set_title(param, fontsize=10)

    fig.tight_layout()
    filename = f"marginal_violin_{scope}_{front_type}_{budget}_{target}.png"
    return save_figure(fig, str(Path("marginal_distribution") / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()

    for budget in BUDGETS:
        stats_rows: list[dict[str, object]] = []
        range_rows: list[dict[str, object]] = []

        for scope, front_type in SLICES:
            print(f"Processing {scope} | {front_type} | budget {budget} ...")
            slice_df = get_final_configs(load_all_runs_with_config(scope, front_type, budget))

            available_params = [p for p in TOP_PARAMETERS if p in slice_df.columns]
            if not available_params:
                print(f"  No key parameters found, skipping.")
                continue

            for target, target_label in TARGETS:
                top, rest = split_top_rest(slice_df, target)

                plot_kde(top, rest, available_params, scope, front_type, budget, target, target_label)
                plot_violin(top, rest, available_params, scope, front_type, budget, target, target_label)

                for param in available_params:
                    row = compute_stats(top, rest, param)
                    row["scope"] = scope
                    row["front_type"] = front_type
                    row["budget"] = budget
                    row["indicator"] = target
                    stats_rows.append(row)

                    top_vals = top[param].dropna()
                    if len(top_vals) >= 2:
                        range_rows.append(
                            {
                                "scope": scope,
                                "front_type": front_type,
                                "budget": budget,
                                "indicator": target,
                                "parameter": param,
                                "recommended_min": float(top_vals.quantile(0.25)),
                                "recommended_max": float(top_vals.quantile(0.75)),
                                "recommended_median": float(top_vals.median()),
                            }
                        )

        table_dir = TABLE_OUTPUT_ROOT / f"budget_{budget}"
        table_dir.mkdir(parents=True, exist_ok=True)

        stats_df = pd.DataFrame(stats_rows)
        if not stats_df.empty:
            col_order = [
                "scope", "front_type", "budget", "indicator", "parameter",
                "top_n", "rest_n", "top_median", "top_q25", "top_q75",
                "rest_median", "rest_q25", "rest_q75", "u_stat", "p_value", "significant",
            ]
            stats_df = stats_df[[c for c in col_order if c in stats_df.columns]]
            stats_path = table_dir / f"marginal_stats_{budget}.csv"
            stats_df.to_csv(stats_path, index=False)
            print(f"Saved: {stats_path}")

        range_df = pd.DataFrame(range_rows)
        if not range_df.empty:
            range_path = table_dir / f"optimal_ranges_{budget}.csv"
            range_df.to_csv(range_path, index=False)
            print(f"Saved: {range_path}")


if __name__ == "__main__":
    main()
