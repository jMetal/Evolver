"""Statistical comparison of final HV between reference and extreme-point fronts."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd
from scipy import stats

from config import BUDGETS, FAMILIES, FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import effect_size_label, report_table_path, vargha_delaney_a
from data_loader import final_best_per_run

TABLE_ROOT = TABLES_DIR / "statistical_comparison"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("statistical_comparison")


def comparison_rows() -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for family in FAMILIES:
        for budget in BUDGETS:
            reference = final_best_per_run(family, "referenceFronts", budget).copy()
            extreme = final_best_per_run(family, "extremePointsFronts", budget).copy()

            hv_reference = -reference["final_best_HVMinus"]
            hv_extreme = -extreme["final_best_HVMinus"]
            u_stat, p_value = stats.mannwhitneyu(
                hv_reference, hv_extreme, alternative="two-sided"
            )
            a_stat = vargha_delaney_a(hv_reference, hv_extreme)

            rows.append(
                {
                    "family": family,
                    "budget": budget,
                    "reference_hv_median": float(hv_reference.median()),
                    "extreme_hv_median": float(hv_extreme.median()),
                    "median_delta_pct": (
                        100.0 * (float(hv_extreme.median()) - float(hv_reference.median()))
                        / abs(float(hv_reference.median()))
                        if float(hv_reference.median()) != 0.0
                        else 0.0
                    ),
                    "u_statistic": float(u_stat),
                    "p_value": float(p_value),
                    "vargha_delaney_a": float(a_stat),
                    "effect_size": effect_size_label(a_stat),
                    "significant": bool(p_value < 0.05),
                    "better_median_front": (
                        "referenceFronts"
                        if float(hv_reference.median()) > float(hv_extreme.median())
                        else "extremePointsFronts"
                    ),
                }
            )
    return pd.DataFrame(rows).sort_values(["family", "budget"]).reset_index(drop=True)


def plot_final_hv_distributions() -> Path:
    fig, axes = plt.subplots(len(FAMILIES), len(BUDGETS), figsize=(16, 8), sharey="row")
    if len(FAMILIES) == 1:
        axes = [axes]

    for row_idx, family in enumerate(FAMILIES):
        for col_idx, budget in enumerate(BUDGETS):
            ax = axes[row_idx][col_idx]
            reference = final_best_per_run(family, "referenceFronts", budget).copy()
            extreme = final_best_per_run(family, "extremePointsFronts", budget).copy()
            data = [
                (-reference["final_best_HVMinus"]).values,
                (-extreme["final_best_HVMinus"]).values,
            ]

            parts = ax.violinplot(data, positions=[1, 2], showmeans=True, showmedians=True)
            for body, color in zip(parts["bodies"], ["#1b9e77", "#d95f02"], strict=False):
                body.set_facecolor(color)
                body.set_alpha(0.55)

            ax.set_xticks([1, 2])
            ax.set_xticklabels(
                [
                    FRONT_TYPE_LABELS["referenceFronts"],
                    FRONT_TYPE_LABELS["extremePointsFronts"],
                ],
                rotation=15,
                ha="right",
            )
            ax.set_title(f"{family} | budget {budget}")
            if col_idx == 0:
                ax.set_ylabel("Final HV")

    fig.suptitle("Final HV by front type", fontsize=13, fontweight="bold")
    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / "final_hv_front_type_comparison.png"))


def main() -> None:
    setup_style()

    comparison_df = comparison_rows()
    comparison_df.to_csv(TABLE_ROOT / "front_type_statistical_comparison.csv", index=False)

    compact_df = comparison_df.rename(
        columns={
            "family": "Benchmark",
            "budget": "Budget",
            "u_statistic": "U",
            "p_value": "p_value",
            "vargha_delaney_a": "A",
            "effect_size": "Effect",
            "significant": "Significant",
        }
    )[
        [
            "Benchmark",
            "Budget",
            "reference_hv_median",
            "extreme_hv_median",
            "median_delta_pct",
            "U",
            "p_value",
            "A",
            "Effect",
            "Significant",
        ]
    ]
    compact_df.to_csv(report_table_path("front_type_statistical_comparison.csv"), index=False)

    figure_path = plot_final_hv_distributions()
    print(f"Saved: {TABLE_ROOT / 'front_type_statistical_comparison.csv'}")
    print(f"Saved: {report_table_path('front_type_statistical_comparison.csv')}")
    print(f"Saved: {figure_path}")


if __name__ == "__main__":
    main()
