"""Parameter association tests against final HV."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

from config import FRONT_TYPE_COLORS, FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import (
    REPRESENTATIVE_CATEGORICALS,
    REPRESENTATIVE_NUMERICALS,
    report_table_path,
)
from data_loader import get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "parameter_association"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("parameter_association")


def load_family_front_data(family: str, front_type: str) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for budget in [1000, 3000, 5000, 7000]:
        final_best = get_final_best_configs(load_all_runs_with_config(family, front_type, budget)).copy()
        final_best["family"] = family
        final_best["front_type"] = front_type
        final_best["budget"] = budget
        final_best["HV"] = -final_best["HVMinus"]
        frames.append(final_best)
    return pd.concat(frames, ignore_index=True)


def spearman_rows(df: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for family in ["RE3D", "RWA3D"]:
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            slice_df = df.loc[(df["family"] == family) & (df["front_type"] == front_type)].copy()
            for parameter in REPRESENTATIVE_NUMERICALS:
                if parameter not in slice_df.columns:
                    continue
                pair_df = slice_df[[parameter, "HV"]].copy()
                pair_df[parameter] = pd.to_numeric(pair_df[parameter], errors="coerce")
                pair_df = pair_df.dropna()
                if len(pair_df) < 3:
                    continue
                rho, p_value = stats.spearmanr(pair_df[parameter], pair_df["HV"])
                rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "parameter": parameter,
                        "rho": float(rho),
                        "p_value": float(p_value),
                        "significant": bool(p_value < 0.05),
                        "n": int(len(pair_df)),
                    }
                )
    return pd.DataFrame(rows).sort_values(["family", "front_type", "parameter"])


def kruskal_rows(df: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for family in ["RE3D", "RWA3D"]:
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            slice_df = df.loc[(df["family"] == family) & (df["front_type"] == front_type)].copy()
            for parameter in REPRESENTATIVE_CATEGORICALS:
                if parameter not in slice_df.columns:
                    continue
                groups = [
                    group["HV"].to_numpy(dtype=float)
                    for _, group in slice_df.groupby(parameter, sort=True)
                    if len(group) >= 2
                ]
                if len(groups) < 2:
                    continue
                h_stat, p_value = stats.kruskal(*groups)
                rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "parameter": parameter,
                        "h_statistic": float(h_stat),
                        "p_value": float(p_value),
                        "significant": bool(p_value < 0.05),
                        "n_groups": int(len(groups)),
                    }
                )
    return pd.DataFrame(rows).sort_values(["family", "front_type", "parameter"])


def plot_family(
    family: str, spearman_df: pd.DataFrame, kruskal_df: pd.DataFrame
) -> Path:
    fig, axes = plt.subplots(1, 2, figsize=(16, 7))
    fig.suptitle(f"Parameter association with final HV | {family}", fontsize=13, fontweight="bold")

    numeric_df = spearman_df.loc[spearman_df["family"] == family].copy()
    categorical_df = kruskal_df.loc[kruskal_df["family"] == family].copy()

    for ax, source_df, value_column, title in [
        (axes[0], numeric_df, "rho", "Spearman rho (numerical)"),
        (axes[1], categorical_df, "h_statistic", "Kruskal-Wallis H (categorical)"),
    ]:
        parameters = source_df["parameter"].drop_duplicates().tolist()
        y = np.arange(len(parameters))
        height = 0.35
        for offset, front_type in [(-0.2, "referenceFronts"), (0.2, "extremePointsFronts")]:
            sub = source_df.loc[source_df["front_type"] == front_type].set_index("parameter")
            values = [float(sub.loc[param, value_column]) if param in sub.index else 0.0 for param in parameters]
            ax.barh(
                y + offset,
                values,
                height=height,
                color=FRONT_TYPE_COLORS[front_type],
                alpha=0.75,
                label=FRONT_TYPE_LABELS[front_type],
            )

            if "significant" in sub.columns:
                for idx, param in enumerate(parameters):
                    if param in sub.index and bool(sub.loc[param, "significant"]):
                        ax.text(values[idx], y[idx] + offset, " *", va="center", ha="left", fontsize=9)

        ax.set_yticks(y)
        ax.set_yticklabels(parameters)
        ax.set_title(title)
        ax.legend(frameon=False, fontsize=9)

    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / f"parameter_association_{family}.png"))


def main() -> None:
    setup_style()

    pooled_df = pd.concat(
        [
            load_family_front_data(family, front_type)
            for family in ["RE3D", "RWA3D"]
            for front_type in ["referenceFronts", "extremePointsFronts"]
        ],
        ignore_index=True,
    )

    spearman_df = spearman_rows(pooled_df)
    kruskal_df = kruskal_rows(pooled_df)
    spearman_df.to_csv(TABLE_ROOT / "spearman_association_all.csv", index=False)
    kruskal_df.to_csv(TABLE_ROOT / "kruskal_association_all.csv", index=False)

    compact_rows: list[dict[str, object]] = []
    for _, row in spearman_df.iterrows():
        compact_rows.append(
            {
                "Benchmark": row["family"],
                "FrontType": FRONT_TYPE_LABELS[str(row["front_type"])],
                "Test": "Spearman",
                "Parameter": row["parameter"],
                "Statistic": row["rho"],
                "p_value": row["p_value"],
                "Significant": row["significant"],
            }
        )
    for _, row in kruskal_df.iterrows():
        compact_rows.append(
            {
                "Benchmark": row["family"],
                "FrontType": FRONT_TYPE_LABELS[str(row["front_type"])],
                "Test": "Kruskal",
                "Parameter": row["parameter"],
                "Statistic": row["h_statistic"],
                "p_value": row["p_value"],
                "Significant": row["significant"],
            }
        )
    compact_df = pd.DataFrame(compact_rows)
    compact_df.to_csv(report_table_path("parameter_association_tests_all.csv"), index=False)

    for family in ["RE3D", "RWA3D"]:
        plot_family(family, spearman_df, kruskal_df)

    print(f"Saved: {TABLE_ROOT / 'spearman_association_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'kruskal_association_all.csv'}")
    print(f"Saved: {report_table_path('parameter_association_tests_all.csv')}")


if __name__ == "__main__":
    main()
