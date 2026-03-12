"""Random-forest predictive importance with MDI, permutation and CV R2."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.inspection import permutation_importance
from sklearn.metrics import r2_score
from sklearn.model_selection import KFold, cross_val_score, train_test_split
from sklearn.pipeline import Pipeline

from config import FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import (
    active_configuration_columns,
    build_model_preprocessor,
    report_table_path,
)
from data_loader import get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "rf_predictive_importance"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("rf_predictive_importance")

RF_KWARGS = {
    "n_estimators": 200,
    "random_state": 42,
    "n_jobs": -1,
}
TOP_K = 12


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


def fit_slice(df: pd.DataFrame) -> tuple[pd.DataFrame, dict[str, float]]:
    config_columns = active_configuration_columns(df)
    preprocessor, feature_names = build_model_preprocessor(df, config_columns)

    X = df[feature_names].copy()
    y = df["HV"].to_numpy(dtype=float)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42
    )

    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("model", RandomForestRegressor(**RF_KWARGS)),
        ]
    )
    pipeline.fit(X_train, y_train)

    transformed_test = pipeline.named_steps["preprocessor"].transform(X_test)
    model = pipeline.named_steps["model"]
    transformed_feature_names = [
        name.split("__", 1)[-1]
        for name in pipeline.named_steps["preprocessor"].get_feature_names_out()
    ]
    mdi = model.feature_importances_
    permutation = permutation_importance(
        model,
        transformed_test,
        y_test,
        n_repeats=20,
        random_state=42,
        n_jobs=-1,
    )

    importance_df = pd.DataFrame(
        {
            "feature": transformed_feature_names,
            "mdi_importance": mdi,
            "permutation_importance_mean": permutation.importances_mean,
            "permutation_importance_std": permutation.importances_std,
        }
    ).sort_values("mdi_importance", ascending=False)
    importance_df["mdi_rank"] = np.arange(1, len(importance_df) + 1)
    importance_df["permutation_rank"] = (
        importance_df["permutation_importance_mean"]
        .rank(method="first", ascending=False)
        .astype(int)
    )

    cv = KFold(n_splits=5, shuffle=True, random_state=42)
    cv_scores = cross_val_score(pipeline, X, y, cv=cv, scoring="r2", n_jobs=1)
    y_pred = pipeline.predict(X_test)

    metrics = {
        "n_samples": int(len(df)),
        "test_r2": float(r2_score(y_test, y_pred)),
        "cv_r2_mean": float(cv_scores.mean()),
        "cv_r2_std": float(cv_scores.std(ddof=0)),
    }
    return importance_df, metrics


def plot_family(family: str, slice_results: dict[str, pd.DataFrame]) -> Path:
    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    fig.suptitle(f"Random-forest predictive importance | {family}", fontsize=13, fontweight="bold")

    for row_idx, front_type in enumerate(["referenceFronts", "extremePointsFronts"]):
        importance_df = slice_results[front_type]
        for col_idx, (column, title) in enumerate(
            [
                ("mdi_importance", "MDI importance"),
                ("permutation_importance_mean", "Permutation importance"),
            ]
        ):
            ax = axes[row_idx][col_idx]
            top = importance_df.nlargest(TOP_K, column).sort_values(column, ascending=True)
            ax.barh(top["feature"], top[column], color="#1b9e77" if front_type == "referenceFronts" else "#d95f02", alpha=0.8)
            ax.set_title(f"{FRONT_TYPE_LABELS[front_type]} | {title}")
            ax.set_xlabel(title)

    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / f"rf_predictive_importance_{family}.png"))


def main() -> None:
    setup_style()

    all_importance_rows: list[pd.DataFrame] = []
    metric_rows: list[dict[str, object]] = []

    for family in ["RE3D", "RWA3D"]:
        slice_results: dict[str, pd.DataFrame] = {}
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            slice_df = load_family_front_data(family, front_type)
            importance_df, metrics = fit_slice(slice_df)
            importance_df["family"] = family
            importance_df["front_type"] = front_type
            all_importance_rows.append(importance_df)
            metric_rows.append(
                {
                    "family": family,
                    "front_type": front_type,
                    **metrics,
                }
            )
            slice_results[front_type] = importance_df
        plot_family(family, slice_results)

    importance_all_df = pd.concat(all_importance_rows, ignore_index=True)
    metrics_df = pd.DataFrame(metric_rows).sort_values(["family", "front_type"])
    importance_all_df.to_csv(TABLE_ROOT / "rf_predictive_importance_all.csv", index=False)
    metrics_df.to_csv(TABLE_ROOT / "rf_predictive_r2_all.csv", index=False)

    compact_df = metrics_df.rename(
        columns={
            "family": "Benchmark",
            "front_type": "FrontType",
            "n_samples": "Samples",
            "test_r2": "TestR2",
            "cv_r2_mean": "CVR2Mean",
            "cv_r2_std": "CVR2Std",
        }
    )
    compact_df["FrontType"] = compact_df["FrontType"].map(FRONT_TYPE_LABELS)
    compact_df.to_csv(report_table_path("rf_predictive_r2_all.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'rf_predictive_importance_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'rf_predictive_r2_all.csv'}")
    print(f"Saved: {report_table_path('rf_predictive_r2_all.csv')}")


if __name__ == "__main__":
    main()
