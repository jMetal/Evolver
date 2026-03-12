"""Consensus component importance across RE3D and RWA3D for each budget."""

from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor

from config import (
    BUDGETS,
    FIGURES_DIR,
    FRONT_TYPE_LABELS,
    FRONT_TYPES,
    TABLES_DIR,
    save_figure,
    setup_style,
)
from data_loader import encode_config_vector, get_final_configs, load_all_runs_with_config

TOP_K = 10
FAMILIES = ["RE3D", "RWA3D"]
TARGETS = [
    ("EP", "#d73027", "EP"),
    ("HVMinus", "#4575b4", "HV$^-$"),
]
RF_KWARGS = {
    "n_estimators": 200,
    "max_depth": 10,
    "random_state": 42,
    "n_jobs": -1,
}
FIGURE_OUTPUT_ROOT = FIGURES_DIR / "component_importance"
TABLE_OUTPUT_ROOT = TABLES_DIR / "component_importance"
FIGURE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
TABLE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def aggregate_family_importance(
    feature_names: list[str], importances: np.ndarray
) -> pd.DataFrame:
    """Collapse one-hot feature importances back to parameter families."""
    grouped: dict[str, float] = {}
    for feature_name, importance in zip(feature_names, importances, strict=False):
        family = feature_name.split("_")[0] if "_" in feature_name else feature_name
        grouped[family] = grouped.get(family, 0.0) + float(importance)

    result = (
        pd.DataFrame(
            [
                {"parameter_family": family, "importance": importance}
                for family, importance in grouped.items()
            ]
        )
        .sort_values("importance", ascending=False)
        .reset_index(drop=True)
    )
    result["rank"] = np.arange(1, len(result) + 1)
    return result


def compute_family_importance(
    family: str, front_type: str, target: str, budget: int
) -> pd.DataFrame:
    """Fit one RF model for one benchmark and target."""
    final_df = get_final_configs(
        load_all_runs_with_config(family, front_type, budget)
    )
    feature_df, feature_names = encode_config_vector(final_df)
    variable_mask = feature_df.std(axis=0) > 0
    filtered = feature_df.loc[:, variable_mask]
    filtered_names = [
        name for name, keep in zip(feature_names, variable_mask, strict=False) if keep
    ]
    if filtered.shape[1] == 0:
        raise ValueError(f"All encoded features are constant for {family} {front_type} {target}.")

    model = RandomForestRegressor(**RF_KWARGS)
    model.fit(filtered.values, final_df[target].values)

    importance_df = aggregate_family_importance(
        filtered_names, model.feature_importances_
    )
    total = float(importance_df["importance"].sum())
    importance_df["normalized_importance"] = (
        importance_df["importance"] / total if total > 0 else 0.0
    )
    importance_df["scope"] = family
    importance_df["front_type"] = front_type
    importance_df["budget"] = budget
    importance_df["indicator"] = target
    importance_df["n_samples"] = len(final_df)
    return importance_df


def build_consensus(
    front_type: str, target: str, budget: int
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Average normalized importances across benchmarks with equal weight."""
    detail_frames = [
        compute_family_importance(family, front_type, target, budget) for family in FAMILIES
    ]
    detail_df = pd.concat(detail_frames, ignore_index=True)

    pivot = (
        detail_df.pivot_table(
            index="parameter_family",
            columns="scope",
            values="normalized_importance",
            fill_value=0.0,
            aggfunc="first",
        )
        .reindex(columns=FAMILIES, fill_value=0.0)
        .reset_index()
    )
    pivot = pivot.rename(
        columns={family: f"{family}_normalized_importance" for family in FAMILIES}
    )

    rank_pivot = (
        detail_df.pivot_table(
            index="parameter_family",
            columns="scope",
            values="rank",
            aggfunc="first",
        )
        .reset_index()
    )
    rank_pivot = rank_pivot.rename(
        columns={family: f"{family}_rank" for family in FAMILIES}
    )

    consensus_df = pivot.merge(rank_pivot, on="parameter_family", how="left")
    normalized_columns = [f"{family}_normalized_importance" for family in FAMILIES]
    consensus_df["consensus_importance"] = consensus_df[normalized_columns].mean(axis=1)
    consensus_df["consensus_rank"] = (
        consensus_df["consensus_importance"].rank(method="dense", ascending=False).astype(int)
    )
    consensus_df["front_type"] = front_type
    consensus_df["budget"] = budget
    consensus_df["indicator"] = target
    consensus_df = consensus_df.sort_values(
        ["consensus_rank", "parameter_family"]
    ).reset_index(drop=True)
    return detail_df, consensus_df


def plot_consensus(front_type: str, consensus_frames: list[pd.DataFrame], budget: int) -> Path:
    """Create one two-panel figure with consensus importances."""
    import matplotlib.pyplot as plt

    fig, axes = plt.subplots(1, 2, figsize=(14, 6))
    fig.suptitle(
        f"Consensus component importance | RE3D + RWA3D | {FRONT_TYPE_LABELS[front_type]} | budget {budget}",
        fontsize=13,
        fontweight="bold",
    )

    for ax, (target, color, label), consensus_df in zip(
        axes, TARGETS, consensus_frames, strict=False
    ):
        top = (
            consensus_df.nsmallest(TOP_K, "consensus_rank")
            .sort_values("consensus_importance", ascending=True)
        )
        ax.barh(top["parameter_family"], top["consensus_importance"], color=color, alpha=0.8)
        ax.set_title(label)
        ax.set_xlabel("Mean normalized RF importance")
        ax.grid(axis="x", alpha=0.25, linestyle="--")

    front_short = "reference" if front_type == "referenceFronts" else "extreme_points"
    filename = f"component_importance_consensus_{front_short}_{budget}.png"
    return save_figure(fig, str(Path("component_importance") / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()
    for budget in BUDGETS:
        detail_rows: list[pd.DataFrame] = []
        consensus_rows: list[pd.DataFrame] = []

        for front_type in FRONT_TYPES:
            consensus_frames: list[pd.DataFrame] = []
            for target, _, _ in TARGETS:
                detail_df, consensus_df = build_consensus(front_type, target, budget)
                detail_rows.append(detail_df)
                consensus_rows.append(consensus_df)
                consensus_frames.append(consensus_df)

            plot_consensus(front_type, consensus_frames, budget)

        detail_summary = pd.concat(detail_rows, ignore_index=True)[
            [
                "scope",
                "front_type",
                "budget",
                "indicator",
                "rank",
                "parameter_family",
                "importance",
                "normalized_importance",
                "n_samples",
            ]
        ]
        consensus_summary = pd.concat(consensus_rows, ignore_index=True)[
            [
                "front_type",
                "budget",
                "indicator",
                "consensus_rank",
                "parameter_family",
                "consensus_importance",
                "RE3D_normalized_importance",
                "RWA3D_normalized_importance",
            ]
        ]
        top3_summary = consensus_summary[consensus_summary["consensus_rank"] <= 3].copy()

        table_dir = TABLE_OUTPUT_ROOT / f"budget_{budget}"
        table_dir.mkdir(parents=True, exist_ok=True)
        detail_path = table_dir / f"component_importance_consensus_detail_{budget}.csv"
        summary_path = table_dir / f"component_importance_consensus_summary_{budget}.csv"
        top3_path = table_dir / f"component_importance_consensus_top3_{budget}.csv"

        detail_summary.to_csv(detail_path, index=False)
        consensus_summary.to_csv(summary_path, index=False)
        top3_summary.to_csv(top3_path, index=False)

        print(f"Saved: {detail_path}")
        print(f"Saved: {summary_path}")
        print(f"Saved: {top3_path}")


if __name__ == "__main__":
    main()
