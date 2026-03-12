"""Numerical parameter distributions by budget and front type."""

from __future__ import annotations

from math import ceil
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import BUDGETS, FRONT_TYPE_COLORS, FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import REPRESENTATIVE_NUMERICALS, report_table_path
from data_loader import get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "numerical_distributions"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("numerical_distributions")


def load_family_data(family: str) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for front_type in ["referenceFronts", "extremePointsFronts"]:
        for budget in BUDGETS:
            final_best = get_final_best_configs(load_all_runs_with_config(family, front_type, budget)).copy()
            final_best["family"] = family
            final_best["front_type"] = front_type
            final_best["budget"] = budget
            frames.append(final_best)
    return pd.concat(frames, ignore_index=True)


def summarize_numeric(df: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for (family, front_type, budget), slice_df in df.groupby(["family", "front_type", "budget"], sort=True):
        for parameter in REPRESENTATIVE_NUMERICALS:
            if parameter not in slice_df.columns:
                continue
            values = pd.to_numeric(slice_df[parameter], errors="coerce").dropna()
            if values.empty:
                continue
            rows.append(
                {
                    "family": family,
                    "front_type": front_type,
                    "budget": budget,
                    "parameter": parameter,
                    "n": int(len(values)),
                    "median": float(values.median()),
                    "q25": float(values.quantile(0.25)),
                    "q75": float(values.quantile(0.75)),
                }
            )
    return pd.DataFrame(rows).sort_values(["family", "parameter", "budget", "front_type"])


def plot_family(raw_df: pd.DataFrame, family: str) -> Path:
    family_df = raw_df.loc[raw_df["family"] == family].copy()
    available = [parameter for parameter in REPRESENTATIVE_NUMERICALS if parameter in family_df.columns]
    ncols = 3
    nrows = ceil(len(available) / ncols)
    fig, axes = plt.subplots(nrows, ncols, figsize=(18, 4.8 * nrows))
    axes = np.atleast_2d(axes)
    fig.suptitle(f"Numerical parameter distributions | {family}", fontsize=13, fontweight="bold")

    positions = []
    for budget_idx, budget in enumerate(BUDGETS):
        base = budget_idx * 3.0
        positions.append(
            {
                "referenceFronts": base - 0.35,
                "extremePointsFronts": base + 0.35,
                "tick": base,
                "label": str(budget),
            }
        )

    for ax, parameter in zip(axes.flat, available, strict=False):
        if parameter not in family_df.columns:
            ax.set_visible(False)
            continue

        parameter_values: list[np.ndarray] = []
        parameter_positions: list[float] = []
        parameter_colors: list[str] = []
        for position in positions:
            budget = int(position["label"])
            for front_type in ["referenceFronts", "extremePointsFronts"]:
                values = pd.to_numeric(
                    family_df.loc[
                        (family_df["budget"] == budget)
                        & (family_df["front_type"] == front_type)
                        & family_df[parameter].notna(),
                        parameter,
                    ],
                    errors="coerce",
                ).dropna()
                if values.empty:
                    continue
                parameter_values.append(values.to_numpy(dtype=float))
                parameter_positions.append(position[front_type])
                parameter_colors.append(FRONT_TYPE_COLORS[front_type])

        if parameter_values:
            parts = ax.boxplot(
                parameter_values,
                positions=parameter_positions,
                widths=0.55,
                patch_artist=True,
                showfliers=False,
            )
            for patch, color in zip(parts["boxes"], parameter_colors, strict=False):
                patch.set_facecolor(color)
                patch.set_alpha(0.5)

        ax.set_xticks([position["tick"] for position in positions])
        ax.set_xticklabels([position["label"] for position in positions])
        ax.set_title(parameter, fontsize=10)
        ax.set_xlabel("Budget")

    for ax in axes.flat[len(available) :]:
        ax.set_visible(False)

    handles = [
        plt.Line2D([0], [0], color=FRONT_TYPE_COLORS[front_type], linewidth=8, alpha=0.5)
        for front_type in ["referenceFronts", "extremePointsFronts"]
    ]
    labels = [FRONT_TYPE_LABELS[front_type] for front_type in ["referenceFronts", "extremePointsFronts"]]
    fig.legend(handles, labels, loc="lower center", ncol=2, fontsize=9, frameon=False)
    fig.tight_layout(rect=(0, 0.04, 1, 1))
    return save_figure(fig, str(FIGURE_ROOT / f"numerical_distributions_{family}.png"))


def main() -> None:
    setup_style()
    family_frames: list[pd.DataFrame] = []

    for family in ["RE3D", "RWA3D"]:
        family_raw_df = load_family_data(family)
        summary_df = summarize_numeric(family_raw_df)
        family_frames.append(summary_df)
        plot_family(family_raw_df, family)

    all_df = pd.concat(family_frames, ignore_index=True)
    all_df.to_csv(TABLE_ROOT / "numerical_parameter_distribution_summary_all.csv", index=False)

    compact_df = all_df.rename(
        columns={
            "family": "Benchmark",
            "front_type": "FrontType",
            "budget": "Budget",
            "parameter": "Parameter",
            "median": "Median",
            "q25": "Q25",
            "q75": "Q75",
        }
    )
    compact_df["FrontType"] = compact_df["FrontType"].map(FRONT_TYPE_LABELS)
    compact_df.to_csv(report_table_path("numerical_parameter_distribution_summary_all.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'numerical_parameter_distribution_summary_all.csv'}")
    print(f"Saved: {report_table_path('numerical_parameter_distribution_summary_all.csv')}")


if __name__ == "__main__":
    main()
