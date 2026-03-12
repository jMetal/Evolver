"""Analyze elite configuration subspaces and derive slice-level recommendations."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import (
    ELITE_QUANTILES,
    ELITE_VIEWS,
    JOINT_VIEW,
    active_configuration_columns,
    elite_subset,
    iter_slices,
    parameter_kind,
    prioritized_parameters,
    safe_category_label,
)
from data_loader import get_final_configs, load_all_runs_with_config

FIGURE_ROOT = Path("elite_subspace")
TABLE_ROOT = TABLES_DIR / "elite_subspace"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)

FIXED_SHARE_THRESHOLD = 0.80
NUMERIC_IQR_SHARE_THRESHOLD = 0.20
MIN_OBSERVED_ROWS = 10
MIN_ELITE_ROWS = 5


def load_slice(family: str, front_type: str, budget: int) -> pd.DataFrame:
    """Load final configurations for one slice with stable row ids."""
    final_df = get_final_configs(load_all_runs_with_config(family, front_type, budget)).copy()
    final_df["row_id"] = np.arange(len(final_df))
    return final_df


def numeric_detail_rows(
    slice_df: pd.DataFrame,
    elite_df: pd.DataFrame,
    family: str,
    front_type: str,
    budget: int,
    view: str,
    quantile: float,
    parameter: str,
) -> dict[str, object] | None:
    """Compute numeric concentration statistics for one parameter."""
    observed = pd.to_numeric(slice_df[parameter], errors="coerce").dropna()
    elite = pd.to_numeric(elite_df[parameter], errors="coerce").dropna()
    if observed.empty:
        return None

    observed_min = float(observed.min())
    observed_max = float(observed.max())
    observed_range = float(observed_max - observed_min)
    elite_q25 = float(elite.quantile(0.25)) if not elite.empty else np.nan
    elite_q75 = float(elite.quantile(0.75)) if not elite.empty else np.nan
    elite_iqr = float(elite_q75 - elite_q25) if not elite.empty else np.nan
    elite_iqr_share = (
        float(elite_iqr / observed_range)
        if observed_range > 0.0 and np.isfinite(elite_iqr)
        else np.nan
    )

    return {
        "family": family,
        "front_type": front_type,
        "budget": budget,
        "view": view,
        "quantile": quantile,
        "parameter": parameter,
        "parameter_kind": "numerical",
        "observed_n": int(len(observed)),
        "elite_n": int(len(elite)),
        "observed_min": observed_min,
        "observed_max": observed_max,
        "observed_range": observed_range,
        "elite_min": float(elite.min()) if not elite.empty else np.nan,
        "elite_q25": elite_q25,
        "elite_median": float(elite.median()) if not elite.empty else np.nan,
        "elite_q75": elite_q75,
        "elite_max": float(elite.max()) if not elite.empty else np.nan,
        "elite_iqr": elite_iqr,
        "elite_iqr_share": elite_iqr_share,
    }


def categorical_rows(
    slice_df: pd.DataFrame,
    elite_df: pd.DataFrame,
    family: str,
    front_type: str,
    budget: int,
    view: str,
    quantile: float,
    parameter: str,
) -> tuple[dict[str, object] | None, list[dict[str, object]]]:
    """Compute categorical concentration summaries and frequencies."""
    observed = slice_df[parameter].map(safe_category_label)
    elite = elite_df[parameter].map(safe_category_label)
    if observed.empty:
        return None, []

    observed_share = observed.value_counts(normalize=True).sort_values(ascending=False)
    elite_share = elite.value_counts(normalize=True).sort_values(ascending=False)
    dominant_category = elite_share.index[0] if not elite_share.empty else "<NA>"
    dominant_share = float(elite_share.iloc[0]) if not elite_share.empty else np.nan

    summary = {
        "family": family,
        "front_type": front_type,
        "budget": budget,
        "view": view,
        "quantile": quantile,
        "parameter": parameter,
        "parameter_kind": "categorical",
        "observed_n": int(len(observed)),
        "elite_n": int(len(elite)),
        "observed_unique": int(observed.nunique(dropna=False)),
        "elite_unique": int(elite.nunique(dropna=False)),
        "dominant_category": dominant_category,
        "dominant_share": dominant_share,
    }

    frequency_rows: list[dict[str, object]] = []
    categories = sorted(set(observed_share.index).union(set(elite_share.index)))
    for category in categories:
        frequency_rows.append(
            {
                "family": family,
                "front_type": front_type,
                "budget": budget,
                "view": view,
                "quantile": quantile,
                "parameter": parameter,
                "category": category,
                "observed_share": float(observed_share.get(category, 0.0)),
                "elite_share": float(elite_share.get(category, 0.0)),
                "elite_over_representation": float(
                    elite_share.get(category, 0.0) - observed_share.get(category, 0.0)
                ),
            }
        )

    return summary, frequency_rows


def overlapping_interval_rows(
    rows: list[dict[str, object] | pd.Series], tolerance: float = 1e-12
) -> list[str]:
    """Return the view names that have overlapping IQR intervals."""
    supported: set[str] = set()

    def _value(row: dict[str, object] | pd.Series, key: str) -> object:
        if isinstance(row, pd.Series):
            return row[key]
        return row[key]

    for index, left in enumerate(rows):
        for right in rows[index + 1 :]:
            overlap_min = max(float(_value(left, "elite_q25")), float(_value(right, "elite_q25")))
            overlap_max = min(float(_value(left, "elite_q75")), float(_value(right, "elite_q75")))
            if overlap_min <= overlap_max + tolerance:
                supported.add(str(_value(left, "view")))
                supported.add(str(_value(right, "view")))
    return sorted(supported)


def recommend_parameter_action(
    family: str,
    front_type: str,
    budget: int,
    parameter: str,
    numeric_df: pd.DataFrame,
    categorical_df: pd.DataFrame,
) -> dict[str, object]:
    """Build one recommendation row for a parameter in a slice."""
    result = {
        "family": family,
        "front_type": front_type,
        "budget": budget,
        "parameter": parameter,
        "parameter_kind": parameter_kind(parameter),
        "action": "keep_open",
        "supported_views": "",
        "recommendation_reason": "insufficient_consistency",
        "recommended_value": None,
        "recommended_min": np.nan,
        "recommended_max": np.nan,
    }

    if parameter_kind(parameter) == "categorical":
        view_rows = categorical_df.loc[
            (categorical_df["parameter"] == parameter)
            & (categorical_df["quantile"] == 0.10)
            & (categorical_df["dominant_share"] >= FIXED_SHARE_THRESHOLD)
            & (categorical_df["elite_n"] >= MIN_ELITE_ROWS)
        ].copy()
        if not view_rows.empty:
            support = (
                view_rows.groupby("dominant_category")["view"]
                .agg(list)
                .sort_values(key=lambda values: values.map(len), ascending=False)
            )
            best_category = str(support.index[0])
            views = support.iloc[0]
            if len(views) >= 2:
                result["action"] = "fix"
                result["supported_views"] = "|".join(sorted(views))
                result["recommendation_reason"] = "dominant_category_repeats"
                result["recommended_value"] = best_category
        return result

    view_rows = numeric_df.loc[
        (numeric_df["parameter"] == parameter)
        & (numeric_df["quantile"] == 0.10)
        & (numeric_df["observed_n"] >= MIN_OBSERVED_ROWS)
        & (numeric_df["elite_n"] >= MIN_ELITE_ROWS)
        & (numeric_df["elite_iqr_share"] <= NUMERIC_IQR_SHARE_THRESHOLD)
    ].copy()
    if len(view_rows) < 2:
        return result

    supported_views = overlapping_interval_rows(view_rows.to_dict("records"))
    if len(supported_views) < 2:
        return result

    joint_row = view_rows.loc[view_rows["view"] == JOINT_VIEW]
    anchor = joint_row.iloc[0] if not joint_row.empty else view_rows.sort_values("elite_iqr_share").iloc[0]
    observed_min = float(anchor["observed_min"])
    observed_max = float(anchor["observed_max"])
    elite_median = float(anchor["elite_median"])
    elite_iqr = float(anchor["elite_iqr"])
    suggested_min = max(observed_min, elite_median - 1.5 * elite_iqr)
    suggested_max = min(observed_max, elite_median + 1.5 * elite_iqr)
    if suggested_min > suggested_max:
        suggested_min = elite_median
        suggested_max = elite_median

    result["action"] = "narrow"
    result["supported_views"] = "|".join(supported_views)
    result["recommendation_reason"] = "low_iqr_and_overlapping_centers"
    result["recommended_min"] = suggested_min
    result["recommended_max"] = suggested_max
    return result


def plot_elite_profile(
    slice_df: pd.DataFrame,
    joint_elite_df: pd.DataFrame,
    recommendation_df: pd.DataFrame,
    family: str,
    front_type: str,
    budget: int,
) -> Path:
    """Plot up to four parameters comparing overall vs joint elite 10%."""
    available_columns = active_configuration_columns(slice_df)
    selected = recommendation_df.loc[
        recommendation_df["action"] != "keep_open", "parameter"
    ].tolist()
    selected.extend(prioritized_parameters(front_type, budget, available_columns, top_k=8))
    parameters: list[str] = []
    for parameter in selected:
        if parameter in available_columns and parameter not in parameters:
            parameters.append(parameter)
        if len(parameters) == 4:
            break
    if not parameters:
        parameters = available_columns[:4]

    nrows = 2
    ncols = 2
    fig, axes = plt.subplots(nrows, ncols, figsize=(12, 9))
    axes_flat = axes.flat if hasattr(axes, "flat") else [axes]
    fig.suptitle(
        f"Elite 10% conjunto vs total | {family} | {FRONT_TYPE_LABELS[front_type]} | budget {budget}",
        fontsize=13,
        fontweight="bold",
    )

    for ax, parameter in zip(axes_flat, parameters, strict=False):
        if parameter_kind(parameter) == "numerical":
            all_values = pd.to_numeric(slice_df[parameter], errors="coerce").dropna()
            elite_values = pd.to_numeric(joint_elite_df[parameter], errors="coerce").dropna()
            if not all_values.empty:
                ax.hist(
                    all_values,
                    bins=20,
                    color="#bdbdbd",
                    alpha=0.55,
                    density=True,
                    label="Total",
                )
            if not elite_values.empty:
                ax.hist(
                    elite_values,
                    bins=12,
                    color="#1b9e77",
                    alpha=0.65,
                    density=True,
                    label="Elite 10%",
                )
                ax.axvline(float(elite_values.median()), color="#1b9e77", linestyle="--", linewidth=1.5)
            ax.set_ylabel("Densidad")
        else:
            all_share = slice_df[parameter].map(safe_category_label).value_counts(normalize=True)
            elite_share = joint_elite_df[parameter].map(safe_category_label).value_counts(normalize=True)
            categories = sorted(set(all_share.index).union(set(elite_share.index)))
            positions = np.arange(len(categories))
            width = 0.38
            ax.bar(
                positions - width / 2,
                [float(all_share.get(category, 0.0)) for category in categories],
                width=width,
                color="#bdbdbd",
                label="Total",
            )
            ax.bar(
                positions + width / 2,
                [float(elite_share.get(category, 0.0)) for category in categories],
                width=width,
                color="#1b9e77",
                label="Elite 10%",
            )
            ax.set_xticks(positions)
            ax.set_xticklabels(categories, rotation=25, ha="right")
            ax.set_ylabel("Frecuencia relativa")

        ax.set_title(parameter, fontsize=10)
        ax.legend(frameon=False, fontsize=8)

    for ax in list(axes_flat)[len(parameters) :]:
        ax.set_visible(False)

    fig.tight_layout()
    filename = f"elite_profile_{family}_{front_type}_{budget}.png"
    return save_figure(fig, str(FIGURE_ROOT / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()

    all_membership_rows: list[dict[str, object]] = []
    all_numeric_rows: list[dict[str, object]] = []
    all_categorical_summary_rows: list[dict[str, object]] = []
    all_categorical_frequency_rows: list[dict[str, object]] = []
    all_recommendations: list[dict[str, object]] = []

    for family, front_type, budget in iter_slices():
        print(f"Processing {family} | {front_type} | budget {budget} ...")
        slice_df = load_slice(family, front_type, budget)
        config_columns = active_configuration_columns(slice_df)
        elite_sets: dict[tuple[str, float], pd.DataFrame] = {}

        for view in ELITE_VIEWS:
            for quantile in ELITE_QUANTILES:
                elite_df = elite_subset(slice_df, view, quantile)
                elite_sets[(view, quantile)] = elite_df
                all_membership_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "view": view,
                        "quantile": quantile,
                        "n_total": int(len(slice_df)),
                        "n_elite": int(len(elite_df)),
                    }
                )

                for parameter in config_columns:
                    if parameter_kind(parameter) == "numerical":
                        row = numeric_detail_rows(
                            slice_df,
                            elite_df,
                            family,
                            front_type,
                            budget,
                            view,
                            quantile,
                            parameter,
                        )
                        if row is not None:
                            all_numeric_rows.append(row)
                    else:
                        summary, frequency_rows = categorical_rows(
                            slice_df,
                            elite_df,
                            family,
                            front_type,
                            budget,
                            view,
                            quantile,
                            parameter,
                        )
                        if summary is not None:
                            all_categorical_summary_rows.append(summary)
                        all_categorical_frequency_rows.extend(frequency_rows)

        numeric_df = pd.DataFrame(all_numeric_rows)
        categorical_df = pd.DataFrame(all_categorical_summary_rows)
        current_numeric = numeric_df.loc[
            (numeric_df["family"] == family)
            & (numeric_df["front_type"] == front_type)
            & (numeric_df["budget"] == budget)
        ].copy()
        current_categorical = categorical_df.loc[
            (categorical_df["family"] == family)
            & (categorical_df["front_type"] == front_type)
            & (categorical_df["budget"] == budget)
        ].copy()

        slice_recommendations = [
            recommend_parameter_action(
                family,
                front_type,
                budget,
                parameter,
                current_numeric,
                current_categorical,
            )
            for parameter in config_columns
        ]
        recommendation_df = pd.DataFrame(slice_recommendations)
        all_recommendations.extend(slice_recommendations)

        joint_elite_df = elite_sets[(JOINT_VIEW, 0.10)]
        figure_path = plot_elite_profile(
            slice_df,
            joint_elite_df,
            recommendation_df,
            family,
            front_type,
            budget,
        )
        print(f"  Saved: {figure_path}")

    membership_df = pd.DataFrame(all_membership_rows).sort_values(
        ["budget", "front_type", "family", "view", "quantile"]
    )
    membership_df["elite_id"] = (
        membership_df["view"] + "_" + membership_df["quantile"].map(lambda value: f"{value:.2f}")
    )
    inclusion_rows: list[dict[str, object]] = []
    for family, front_type, budget in iter_slices():
        slice_df = load_slice(family, front_type, budget)
        for view in ELITE_VIEWS:
            top5 = set(elite_subset(slice_df, view, 0.05)["row_id"])
            top10 = set(elite_subset(slice_df, view, 0.10)["row_id"])
            top20 = set(elite_subset(slice_df, view, 0.20)["row_id"])
            inclusion_rows.append(
                {
                    "family": family,
                    "front_type": front_type,
                    "budget": budget,
                    "view": view,
                    "top5_within_top10": top5.issubset(top10),
                    "top10_within_top20": top10.issubset(top20),
                }
            )

    all_numeric_df = pd.DataFrame(all_numeric_rows).sort_values(
        ["budget", "front_type", "family", "parameter", "view", "quantile"]
    )
    all_categorical_summary_df = pd.DataFrame(all_categorical_summary_rows).sort_values(
        ["budget", "front_type", "family", "parameter", "view", "quantile"]
    )
    all_categorical_frequency_df = pd.DataFrame(all_categorical_frequency_rows).sort_values(
        ["budget", "front_type", "family", "parameter", "view", "quantile", "category"]
    )
    all_recommendations_df = pd.DataFrame(all_recommendations).sort_values(
        ["budget", "front_type", "family", "action", "parameter"]
    )
    inclusion_df = pd.DataFrame(inclusion_rows).sort_values(
        ["budget", "front_type", "family", "view"]
    )

    membership_df.to_csv(TABLE_ROOT / "elite_membership_all.csv", index=False)
    inclusion_df.to_csv(TABLE_ROOT / "elite_consistency_checks.csv", index=False)
    all_numeric_df.to_csv(TABLE_ROOT / "elite_numeric_detail_all.csv", index=False)
    all_categorical_summary_df.to_csv(TABLE_ROOT / "elite_categorical_summary_all.csv", index=False)
    all_categorical_frequency_df.to_csv(TABLE_ROOT / "elite_categorical_frequency_all.csv", index=False)
    all_recommendations_df.to_csv(TABLE_ROOT / "elite_recommendations_all.csv", index=False)

    for budget in sorted(all_recommendations_df["budget"].unique()):
        budget_dir = TABLE_ROOT / f"budget_{budget}"
        budget_dir.mkdir(parents=True, exist_ok=True)
        membership_df.loc[membership_df["budget"] == budget].to_csv(
            budget_dir / f"elite_membership_{budget}.csv",
            index=False,
        )
        all_numeric_df.loc[all_numeric_df["budget"] == budget].to_csv(
            budget_dir / f"elite_numeric_detail_{budget}.csv",
            index=False,
        )
        all_categorical_summary_df.loc[all_categorical_summary_df["budget"] == budget].to_csv(
            budget_dir / f"elite_categorical_summary_{budget}.csv",
            index=False,
        )
        all_categorical_frequency_df.loc[
            all_categorical_frequency_df["budget"] == budget
        ].to_csv(
            budget_dir / f"elite_categorical_frequency_{budget}.csv",
            index=False,
        )
        all_recommendations_df.loc[all_recommendations_df["budget"] == budget].to_csv(
            budget_dir / f"elite_recommendations_{budget}.csv",
            index=False,
        )

    print(f"Saved: {TABLE_ROOT / 'elite_membership_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'elite_consistency_checks.csv'}")
    print(f"Saved: {TABLE_ROOT / 'elite_numeric_detail_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'elite_categorical_summary_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'elite_categorical_frequency_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'elite_recommendations_all.csv'}")


if __name__ == "__main__":
    main()
