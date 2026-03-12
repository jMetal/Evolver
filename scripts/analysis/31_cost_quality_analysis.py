"""Cost-quality analysis based on running-best HV percentages."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import report_table_path
from data_loader import best_per_evaluation, final_best_per_run, load_all_runs

TABLE_ROOT = TABLES_DIR / "cost_quality"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("cost_quality")


def reference_hv_by_family(family: str) -> float:
    frames: list[pd.DataFrame] = []
    for front_type in ["referenceFronts", "extremePointsFronts"]:
        final_df = final_best_per_run(family, front_type, 7000).copy()
        frames.append(final_df)
    pooled = pd.concat(frames, ignore_index=True)
    return float((-pooled["final_best_HVMinus"]).median())


def slice_running_best(family: str, front_type: str, budget: int) -> pd.DataFrame:
    best = best_per_evaluation(load_all_runs(family, front_type, budget)).copy()
    best["running_best_HV"] = (
        best.sort_values(["run", "Evaluation"])
        .groupby("run")["best_HVMinus"]
        .cummin()
        .mul(-1.0)
    )
    summary = (
        best.groupby("Evaluation", as_index=False)["running_best_HV"]
        .median()
        .rename(columns={"running_best_HV": "median_running_best_hv"})
    )
    summary["family"] = family
    summary["front_type"] = front_type
    summary["budget"] = budget
    summary["total_cost"] = summary["Evaluation"] * budget
    return summary


def pareto_front(df: pd.DataFrame) -> pd.DataFrame:
    ordered = df.sort_values(["total_cost", "pct_reference_hv"], ascending=[True, False]).copy()
    running_best = -np.inf
    keep_rows: list[int] = []
    for index, row in ordered.iterrows():
        if float(row["pct_reference_hv"]) > running_best:
            keep_rows.append(index)
            running_best = float(row["pct_reference_hv"])
    return ordered.loc[keep_rows].reset_index(drop=True)


def plot_family(family_df: pd.DataFrame, family: str) -> Path:
    fig, axes = plt.subplots(1, 2, figsize=(16, 5), sharey=True)
    fig.suptitle(f"Cost-quality heatmaps | {family}", fontsize=13, fontweight="bold")

    for ax, front_type in zip(axes, ["referenceFronts", "extremePointsFronts"], strict=False):
        sub = family_df.loc[family_df["front_type"] == front_type].copy()
        matrix = (
            sub.pivot(index="budget", columns="Evaluation", values="pct_reference_hv")
            .sort_index()
            .sort_index(axis=1)
        )
        im = ax.imshow(matrix.to_numpy(dtype=float), aspect="auto", cmap="YlGnBu", vmin=0.0, vmax=max(100.0, float(np.nanmax(matrix.to_numpy(dtype=float)))))
        ax.set_yticks(np.arange(len(matrix.index)))
        ax.set_yticklabels([str(value) for value in matrix.index])
        xtick_positions = np.arange(0, len(matrix.columns), 4)
        ax.set_xticks(xtick_positions)
        ax.set_xticklabels([str(matrix.columns[idx]) for idx in xtick_positions], rotation=45, ha="right")
        ax.set_title(FRONT_TYPE_LABELS[front_type])
        ax.set_xlabel("Meta-evaluation")
        ax.set_ylabel("Budget")
        fig.colorbar(im, ax=ax, fraction=0.046, pad=0.02, label="% reference HV")

    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / f"cost_quality_{family}.png"))


def main() -> None:
    setup_style()

    detail_rows: list[pd.DataFrame] = []
    threshold_rows: list[dict[str, object]] = []
    pareto_rows: list[pd.DataFrame] = []
    pooled_threshold_rows: list[dict[str, object]] = []

    for family in ["RE3D", "RWA3D"]:
        reference_hv = reference_hv_by_family(family)
        family_frames: list[pd.DataFrame] = []
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            for budget in [1000, 3000, 5000, 7000]:
                summary = slice_running_best(family, front_type, budget).copy()
                summary["reference_hv"] = reference_hv
                summary["pct_reference_hv"] = 100.0 * summary["median_running_best_hv"] / reference_hv if reference_hv != 0.0 else 0.0
                family_frames.append(summary)

                for target in [90.0, 95.0, 99.0, 99.9]:
                    reached = summary.loc[summary["pct_reference_hv"] >= target].sort_values("total_cost").head(1)
                    threshold_rows.append(
                        {
                            "family": family,
                            "front_type": front_type,
                            "target_pct_reference_hv": target,
                            "budget": int(reached["budget"].iloc[0]) if not reached.empty else np.nan,
                            "evaluation": int(reached["Evaluation"].iloc[0]) if not reached.empty else np.nan,
                            "total_cost": int(reached["total_cost"].iloc[0]) if not reached.empty else np.nan,
                            "pct_reference_hv": float(reached["pct_reference_hv"].iloc[0]) if not reached.empty else np.nan,
                        }
                    )

        family_df = pd.concat(family_frames, ignore_index=True)
        detail_rows.append(family_df)
        plot_family(family_df, family)

        pooled = family_df.sort_values("total_cost").copy()
        for target in [90.0, 95.0, 99.0, 99.9]:
            reached = pooled.loc[pooled["pct_reference_hv"] >= target].sort_values("total_cost").head(1)
            pooled_threshold_rows.append(
                {
                    "family": family,
                    "target_pct_reference_hv": target,
                    "front_type": reached["front_type"].iloc[0] if not reached.empty else np.nan,
                    "budget": int(reached["budget"].iloc[0]) if not reached.empty else np.nan,
                    "evaluation": int(reached["Evaluation"].iloc[0]) if not reached.empty else np.nan,
                    "total_cost": int(reached["total_cost"].iloc[0]) if not reached.empty else np.nan,
                    "pct_reference_hv": float(reached["pct_reference_hv"].iloc[0]) if not reached.empty else np.nan,
                }
            )

        pareto = pareto_front(family_df[["family", "front_type", "budget", "Evaluation", "total_cost", "pct_reference_hv"]])
        pareto_rows.append(pareto)

    detail_df = pd.concat(detail_rows, ignore_index=True)
    threshold_df = pd.DataFrame(threshold_rows).sort_values(["family", "front_type", "target_pct_reference_hv"])
    pooled_threshold_df = pd.DataFrame(pooled_threshold_rows).sort_values(["family", "target_pct_reference_hv"])
    pareto_df = pd.concat(pareto_rows, ignore_index=True)

    detail_df.to_csv(TABLE_ROOT / "cost_quality_detail.csv", index=False)
    threshold_df.to_csv(TABLE_ROOT / "cost_quality_thresholds_by_front.csv", index=False)
    pooled_threshold_df.to_csv(TABLE_ROOT / "cost_quality_thresholds_pooled.csv", index=False)
    pareto_df.to_csv(TABLE_ROOT / "cost_quality_pareto_front.csv", index=False)

    compact_df = pooled_threshold_df.copy()
    compact_df["front_type"] = compact_df["front_type"].map(FRONT_TYPE_LABELS)
    compact_df.to_csv(report_table_path("cost_quality_thresholds_pooled.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'cost_quality_detail.csv'}")
    print(f"Saved: {TABLE_ROOT / 'cost_quality_thresholds_by_front.csv'}")
    print(f"Saved: {TABLE_ROOT / 'cost_quality_thresholds_pooled.csv'}")
    print(f"Saved: {TABLE_ROOT / 'cost_quality_pareto_front.csv'}")
    print(f"Saved: {report_table_path('cost_quality_thresholds_pooled.csv')}")


if __name__ == "__main__":
    main()
