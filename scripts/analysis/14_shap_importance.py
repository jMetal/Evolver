"""SHAP-based feature importance showing direction, magnitude, and interactions."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import shap
from sklearn.ensemble import RandomForestRegressor

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
    (family, front_type)
    for front_type in FRONT_TYPES
    for family in ["RE3D", "RWA3D"]
]
TARGETS = [
    ("EP", "#d73027", "EP"),
    ("HVMinus", "#4575b4", "HV$^-$"),
]
FIGURE_OUTPUT_ROOT = FIGURES_DIR / "shap_importance"
TABLE_OUTPUT_ROOT = TABLES_DIR / "shap_importance"
FIGURE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
TABLE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def fit_and_explain(
    slice_df: pd.DataFrame, target: str
) -> tuple[np.ndarray, pd.DataFrame, list[str]]:
    """Fit RF and compute SHAP values, returning (shap_values, feature_df, feature_names)."""
    feature_df, feature_names = encode_config_vector(slice_df)
    variable_mask = feature_df.std(axis=0) > 0
    feature_df = feature_df.loc[:, variable_mask]
    feature_names = [n for n, keep in zip(feature_names, variable_mask, strict=False) if keep]

    model = RandomForestRegressor(**RF_KWARGS)
    model.fit(feature_df.values, slice_df[target].values)

    explainer = shap.TreeExplainer(model)
    shap_values = explainer.shap_values(feature_df)
    return shap_values, feature_df, feature_names


def aggregate_shap_to_families(
    shap_values: np.ndarray, feature_names: list[str]
) -> pd.DataFrame:
    """Aggregate mean|SHAP| from one-hot features to parameter families."""
    mean_abs = np.abs(shap_values).mean(axis=0)
    grouped: dict[str, float] = {}
    for name, val in zip(feature_names, mean_abs, strict=False):
        family = name.split("_")[0] if "_" in name else name
        grouped[family] = grouped.get(family, 0.0) + float(val)

    rows = [
        {"parameter_family": fam, "mean_abs_shap": imp}
        for fam, imp in grouped.items()
    ]
    result = pd.DataFrame(rows).sort_values("mean_abs_shap", ascending=False).reset_index(drop=True)
    result["rank"] = np.arange(1, len(result) + 1)
    return result


def plot_beeswarm(
    shap_values: np.ndarray,
    feature_df: pd.DataFrame,
    scope: str,
    front_type: str,
    budget: int,
    target: str,
    target_label: str,
) -> Path:
    """SHAP beeswarm plot showing direction and magnitude."""
    fig = plt.figure(figsize=(10, 8))
    shap.summary_plot(
        shap_values,
        feature_df,
        show=False,
        max_display=TOP_K,
        plot_size=None,
    )
    plt.title(
        f"SHAP beeswarm | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | {target_label}",
        fontsize=11,
    )
    plt.tight_layout()
    fig = plt.gcf()
    filename = f"shap_beeswarm_{scope}_{front_type}_{budget}_{target}.png"
    return save_figure(fig, str(Path("shap_importance") / f"budget_{budget}" / filename))


def plot_bar(
    shap_values: np.ndarray,
    feature_df: pd.DataFrame,
    scope: str,
    front_type: str,
    budget: int,
    target: str,
    target_label: str,
) -> Path:
    """SHAP bar plot showing global importance."""
    fig = plt.figure(figsize=(10, 8))
    shap.summary_plot(
        shap_values,
        feature_df,
        plot_type="bar",
        show=False,
        max_display=TOP_K,
        plot_size=None,
    )
    plt.title(
        f"SHAP bar | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | {target_label}",
        fontsize=11,
    )
    plt.tight_layout()
    fig = plt.gcf()
    filename = f"shap_bar_{scope}_{front_type}_{budget}_{target}.png"
    return save_figure(fig, str(Path("shap_importance") / f"budget_{budget}" / filename))


def plot_dependence(
    shap_values: np.ndarray,
    feature_df: pd.DataFrame,
    feature_names: list[str],
    scope: str,
    front_type: str,
    budget: int,
    target: str,
    target_label: str,
) -> list[Path]:
    """Dependence plots for TOP_PARAMETERS."""
    paths: list[Path] = []
    for param in TOP_PARAMETERS:
        matching = [n for n in feature_names if n == param or n.startswith(param + "_")]
        if not matching:
            continue
        col = matching[0]
        if col not in feature_df.columns:
            continue
        col_idx = feature_names.index(col)

        fig, ax = plt.subplots(figsize=(8, 6))
        shap.dependence_plot(
            col_idx,
            shap_values,
            feature_df,
            show=False,
            ax=ax,
        )
        ax.set_title(
            f"SHAP dep: {param} | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | {target_label}",
            fontsize=10,
        )
        fig.tight_layout()
        filename = f"shap_dep_{param}_{scope}_{front_type}_{budget}_{target}.png"
        paths.append(save_figure(fig, str(Path("shap_importance") / f"budget_{budget}" / filename)))
    return paths


def main() -> None:
    setup_style()

    for budget in BUDGETS:
        summary_rows: list[dict[str, object]] = []

        for scope, front_type in SLICES:
            print(f"Processing {scope} | {front_type} | budget {budget} ...")
            slice_df = get_final_configs(load_all_runs_with_config(scope, front_type, budget))

            for target, _, target_label in TARGETS:
                shap_values, feature_df, feature_names = fit_and_explain(slice_df, target)

                plot_beeswarm(shap_values, feature_df, scope, front_type, budget, target, target_label)
                plot_bar(shap_values, feature_df, scope, front_type, budget, target, target_label)
                plot_dependence(shap_values, feature_df, feature_names, scope, front_type, budget, target, target_label)

                family_df = aggregate_shap_to_families(shap_values, feature_names)
                family_df["scope"] = scope
                family_df["front_type"] = front_type
                family_df["budget"] = budget
                family_df["indicator"] = target
                family_df["n_samples"] = len(slice_df)
                summary_rows.extend(family_df.to_dict("records"))

        table_dir = TABLE_OUTPUT_ROOT / f"budget_{budget}"
        table_dir.mkdir(parents=True, exist_ok=True)

        summary = pd.DataFrame(summary_rows)
        if not summary.empty:
            col_order = [
                "scope", "front_type", "budget", "indicator", "rank",
                "parameter_family", "mean_abs_shap", "n_samples",
            ]
            summary = summary[[c for c in col_order if c in summary.columns]]
            summary_path = table_dir / f"shap_importance_summary_{budget}.csv"
            summary.to_csv(summary_path, index=False)
            print(f"Saved: {summary_path}")

            top3 = summary[summary["rank"] <= 3].copy()
            top3_path = table_dir / f"shap_importance_top3_{budget}.csv"
            top3.to_csv(top3_path, index=False)
            print(f"Saved: {top3_path}")


if __name__ == "__main__":
    main()
