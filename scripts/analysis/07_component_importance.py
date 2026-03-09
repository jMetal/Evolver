"""Random-forest component importance for the final configurations of each budget."""

from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor

from config import BUDGETS, FIGURES_DIR, FRONT_TYPE_LABELS, RESULTS_ROOT, TABLES_DIR, setup_style, save_figure
from data_loader import (
    encode_config_vector,
    get_final_configs,
    load_all_runs_with_config,
)

TOP_K = 10
RF_KWARGS = {
    "n_estimators": 200,
    "max_depth": 10,
    "random_state": 42,
    "n_jobs": -1,
}
SLICES = [
    ("RE3D", "referenceFronts"),
    ("RWA3D", "referenceFronts"),
    ("RE3D", "estimatedReferenceFronts"),
    ("RWA3D", "estimatedReferenceFronts"),
]
TARGETS = [
    ("EP", "#d73027", "EP"),
    ("HVMinus", "#4575b4", "HV$^-$"),
]
FIGURE_OUTPUT_ROOT = FIGURES_DIR / "component_importance"
TABLE_OUTPUT_ROOT = TABLES_DIR / "component_importance"
FIGURE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
TABLE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def load_slice(scope: str, front_type: str, budget: int) -> pd.DataFrame:
    """Load the final configurations for one analysis slice."""
    return get_final_configs(
        load_all_runs_with_config(scope, front_type, budget, RESULTS_ROOT)
    )


def aggregate_family_importance(feature_names: list[str], importances: np.ndarray) -> pd.DataFrame:
    """Sum one-hot feature importances back to their parameter family."""
    grouped: dict[str, float] = {}
    for feature_name, importance in zip(feature_names, importances, strict=False):
        family = feature_name.split("_")[0] if "_" in feature_name else feature_name
        grouped[family] = grouped.get(family, 0.0) + float(importance)

    rows = [
        {"parameter_family": family, "importance": importance}
        for family, importance in grouped.items()
    ]
    result = pd.DataFrame(rows).sort_values("importance", ascending=False).reset_index(drop=True)
    result["rank"] = np.arange(1, len(result) + 1)
    return result


def compute_importance(slice_df: pd.DataFrame, target: str) -> pd.DataFrame:
    """Fit one RF model and return parameter-family importance."""
    feature_df, feature_names = encode_config_vector(slice_df)
    variable_mask = feature_df.std(axis=0) > 0
    filtered = feature_df.loc[:, variable_mask]
    filtered_names = [name for name, keep in zip(feature_names, variable_mask, strict=False) if keep]
    if filtered.shape[1] == 0:
        raise ValueError(f"All encoded features are constant for target {target}.")

    model = RandomForestRegressor(**RF_KWARGS)
    model.fit(filtered.values, slice_df[target].values)
    return aggregate_family_importance(filtered_names, model.feature_importances_)


def plot_slice(scope: str, front_type: str, rows: list[pd.DataFrame], budget: int) -> Path:
    """Create one two-panel figure for a slice."""
    import matplotlib.pyplot as plt

    fig, axes = plt.subplots(1, 2, figsize=(14, 6))
    fig.suptitle(
        f"Component importance | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget}",
        fontsize=13,
        fontweight="bold",
    )

    for ax, (target, color, title), importance_df in zip(axes, TARGETS, rows, strict=False):
        top = importance_df.nsmallest(TOP_K, "rank").sort_values("importance", ascending=True)
        ax.barh(top["parameter_family"], top["importance"], color=color, alpha=0.8)
        ax.set_title(title)
        ax.set_xlabel("RF importance")
        ax.grid(axis="x", alpha=0.25, linestyle="--")

    filename = f"component_importance_{scope}_{front_type}_{budget}.png"
    return save_figure(fig, str(Path("component_importance") / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()
    for budget in BUDGETS:
        summary_rows: list[dict[str, object]] = []

        for scope, front_type in SLICES:
            slice_df = load_slice(scope, front_type, budget)
            slice_importance: list[pd.DataFrame] = []

            for target, _, _ in TARGETS:
                importance_df = compute_importance(slice_df, target)
                importance_df["scope"] = scope
                importance_df["front_type"] = front_type
                importance_df["budget"] = budget
                importance_df["indicator"] = target
                importance_df["n_samples"] = len(slice_df)
                summary_rows.extend(importance_df.to_dict("records"))
                slice_importance.append(importance_df)

            plot_slice(scope, front_type, slice_importance, budget)

        summary = pd.DataFrame(summary_rows)[
            ["scope", "front_type", "budget", "indicator", "rank", "parameter_family", "importance", "n_samples"]
        ]
        table_dir = TABLE_OUTPUT_ROOT / f"budget_{budget}"
        table_dir.mkdir(parents=True, exist_ok=True)
        summary_path = table_dir / f"component_importance_summary_{budget}.csv"
        summary.to_csv(summary_path, index=False)

        top3 = summary[summary["rank"] <= 3].copy()
        top3_path = table_dir / f"component_importance_top3_{budget}.csv"
        top3.to_csv(top3_path, index=False)

        print(f"Saved: {summary_path}")
        print(f"Saved: {top3_path}")


if __name__ == "__main__":
    main()
