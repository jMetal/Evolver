"""Measure how different final configurations are using Gower distance."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd

from config import (
    BUDGETS,
    CATEGORICAL_COLUMNS,
    FIGURES_DIR,
    FRONT_TYPE_LABELS,
    FRONT_TYPES,
    NUMERICAL_COLUMNS,
    RESULTS_ROOT,
    TABLES_DIR,
    save_figure,
    setup_style,
)
from data_loader import (
    build_config_signature,
    get_configuration_columns,
    get_final_configs,
    load_all_runs_with_config,
)

import matplotlib.pyplot as plt

FAMILIES = ["RE3D", "RWA3D"]
TOP_K = 20
MEDOID_K = 3
TABLES_ROOT = TABLES_DIR / "configuration_distance"
FIGURES_ROOT = FIGURES_DIR / "configuration_distance"
COMPONENT_IMPORTANCE_ROOT = TABLES_DIR / "component_importance"


@dataclass(frozen=True)
class SliceData:
    front_type: str
    budget: int
    re3d: pd.DataFrame
    rwa3d: pd.DataFrame
    pooled: pd.DataFrame
    config_columns: list[str]


def load_final_slice(front_type: str, budget: int) -> SliceData:
    """Load final configurations for both families in one front/budget slice."""
    frames: list[pd.DataFrame] = []
    for family in FAMILIES:
        final_df = get_final_configs(
            load_all_runs_with_config(family, front_type, budget, RESULTS_ROOT)
        ).copy()
        final_df["family"] = family
        final_df["config_signature"] = build_config_signature(final_df)
        final_df["config_id"] = (
            final_df["family"]
            + "_run"
            + final_df["run"].astype(str)
            + "_solution"
            + final_df["SolutionId"].astype(str)
        )
        frames.append(final_df)

    pooled = pd.concat(frames, ignore_index=True)
    config_columns = [
        column for column in get_configuration_columns(pooled) if column != "config_id"
    ]
    return SliceData(
        front_type=front_type,
        budget=budget,
        re3d=pooled.loc[pooled["family"] == "RE3D"].reset_index(drop=True),
        rwa3d=pooled.loc[pooled["family"] == "RWA3D"].reset_index(drop=True),
        pooled=pooled.reset_index(drop=True),
        config_columns=config_columns,
    )


def normalization_ranges(df: pd.DataFrame, config_columns: list[str]) -> dict[str, float]:
    """Compute pooled numeric ranges for Gower normalization."""
    ranges: dict[str, float] = {}
    for column in config_columns:
        if column in NUMERICAL_COLUMNS:
            values = pd.to_numeric(df[column], errors="coerce")
            column_range = float(values.max() - values.min())
            ranges[column] = column_range if np.isfinite(column_range) else 0.0
    return ranges


def consensus_mean_weights(front_type: str, budget: int, config_columns: list[str]) -> pd.DataFrame:
    """Load consensus importances and build one mean weight per configuration column."""
    path = (
        COMPONENT_IMPORTANCE_ROOT
        / f"budget_{budget}"
        / f"component_importance_consensus_summary_{budget}.csv"
    )
    summary = pd.read_csv(path)
    summary = summary.loc[summary["front_type"] == front_type].copy()

    pivot = (
        summary.pivot_table(
            index="parameter_family",
            columns="indicator",
            values="consensus_importance",
            fill_value=0.0,
        )
        .rename_axis(None, axis=1)
        .reset_index()
    )
    if "HVMinus" not in pivot.columns:
        pivot["HVMinus"] = 0.0
    if "EP" not in pivot.columns:
        pivot["EP"] = 0.0
    pivot["mean_weight"] = 0.5 * (pivot["EP"] + pivot["HVMinus"])

    weights = pd.DataFrame({"parameter_family": config_columns}).merge(
        pivot[["parameter_family", "EP", "HVMinus", "mean_weight"]],
        on="parameter_family",
        how="left",
    )
    weights[["EP", "HVMinus", "mean_weight"]] = weights[
        ["EP", "HVMinus", "mean_weight"]
    ].fillna(0.0)
    total = float(weights["mean_weight"].sum())
    if total > 0.0:
        weights["mean_weight"] = weights["mean_weight"] / total
    else:
        weights["mean_weight"] = 1.0 / len(weights)

    weights["front_type"] = front_type
    weights["budget"] = budget
    return weights[
        ["front_type", "budget", "parameter_family", "EP", "HVMinus", "mean_weight"]
    ]


def pairwise_gower(
    left: pd.DataFrame,
    right: pd.DataFrame,
    config_columns: list[str],
    pooled_ranges: dict[str, float],
    weights: dict[str, float] | None = None,
) -> np.ndarray:
    """Compute a mixed-type Gower distance matrix."""
    if weights is None:
        weights = {column: 1.0 for column in config_columns}

    total_weight = float(sum(weights.get(column, 0.0) for column in config_columns))
    if total_weight <= 0.0:
        raise ValueError("Total weight must be positive.")

    distances = np.zeros((len(left), len(right)), dtype=float)

    for column in config_columns:
        weight = float(weights.get(column, 0.0))
        if weight <= 0.0:
            continue

        if column in NUMERICAL_COLUMNS:
            left_values = pd.to_numeric(left[column], errors="coerce").to_numpy(dtype=float)
            right_values = pd.to_numeric(right[column], errors="coerce").to_numpy(dtype=float)
            left_valid = np.isfinite(left_values)
            right_valid = np.isfinite(right_values)

            one_missing = np.logical_xor.outer(~left_valid, ~right_valid).astype(float)
            both_present = np.outer(left_valid, right_valid)
            delta = one_missing

            column_range = pooled_ranges.get(column, 0.0)
            if column_range > 0.0:
                normalized = np.abs(np.subtract.outer(left_values, right_values)) / column_range
                delta = delta + np.where(both_present, normalized, 0.0)
        else:
            left_values = left[column].where(left[column].notna(), None).to_numpy(dtype=object)
            right_values = right[column].where(right[column].notna(), None).to_numpy(dtype=object)
            left_missing = np.equal(left_values, None)
            right_missing = np.equal(right_values, None)
            one_missing = np.logical_xor.outer(left_missing, right_missing).astype(float)
            both_present = np.outer(~left_missing, ~right_missing)
            different = np.not_equal.outer(left_values, right_values).astype(float)
            delta = one_missing + np.where(both_present, different, 0.0)

        distances += weight * delta

    return distances / total_weight


def upper_triangle_values(matrix: np.ndarray) -> np.ndarray:
    """Return the strictly upper-triangular values of a square matrix."""
    if matrix.shape[0] != matrix.shape[1]:
        raise ValueError("Upper triangle is only defined for square matrices.")
    tri_upper = np.triu_indices(matrix.shape[0], k=1)
    return matrix[tri_upper]


def distance_summary_row(
    front_type: str,
    budget: int,
    scheme: str,
    re3d_matrix: np.ndarray,
    rwa_matrix: np.ndarray,
    between_matrix: np.ndarray,
) -> dict[str, float | int | str]:
    """Summarize within/between distances for one slice and scheme."""
    within_re3d = upper_triangle_values(re3d_matrix)
    within_rwa = upper_triangle_values(rwa_matrix)
    between = between_matrix.ravel()

    nearest_re3d = between_matrix.min(axis=1)
    nearest_rwa = between_matrix.min(axis=0)
    mean_within = float(np.mean([within_re3d.mean(), within_rwa.mean()]))

    return {
        "front_type": front_type,
        "budget": budget,
        "scheme": scheme,
        "n_RE3D": int(re3d_matrix.shape[0]),
        "n_RWA3D": int(rwa_matrix.shape[0]),
        "within_RE3D_mean": float(within_re3d.mean()),
        "within_RE3D_median": float(np.median(within_re3d)),
        "within_RWA3D_mean": float(within_rwa.mean()),
        "within_RWA3D_median": float(np.median(within_rwa)),
        "between_mean": float(between.mean()),
        "between_median": float(np.median(between)),
        "nearest_cross_RE3D_mean": float(nearest_re3d.mean()),
        "nearest_cross_RE3D_median": float(np.median(nearest_re3d)),
        "nearest_cross_RWA3D_mean": float(nearest_rwa.mean()),
        "nearest_cross_RWA3D_median": float(np.median(nearest_rwa)),
        "between_minus_mean_within": float(between.mean() - mean_within),
    }


def best_rows_by_indicator(df: pd.DataFrame) -> dict[str, pd.Series]:
    """Return the best final configuration row for EP and HV."""
    best_ep = df.sort_values(["EP", "HVMinus", "run", "SolutionId"]).iloc[0]
    best_hv = df.sort_values(["HVMinus", "EP", "run", "SolutionId"]).iloc[0]
    return {"EP": best_ep, "HV": best_hv}


def best_config_distance_rows(
    slice_data: SliceData,
    pooled_ranges: dict[str, float],
    scheme: str,
    weights: dict[str, float] | None,
) -> pd.DataFrame:
    """Measure distances between the best cross-benchmark configurations."""
    best_rows = {
        "RE3D": best_rows_by_indicator(slice_data.re3d),
        "RWA3D": best_rows_by_indicator(slice_data.rwa3d),
    }
    pairs: list[dict[str, object]] = []
    for left_indicator in ["EP", "HV"]:
        for right_indicator in ["EP", "HV"]:
            left = pd.DataFrame([best_rows["RE3D"][left_indicator]])
            right = pd.DataFrame([best_rows["RWA3D"][right_indicator]])
            distance = pairwise_gower(
                left,
                right,
                slice_data.config_columns,
                pooled_ranges,
                weights=weights,
            )[0, 0]
            pairs.append(
                {
                    "front_type": slice_data.front_type,
                    "budget": slice_data.budget,
                    "scheme": scheme,
                    "pair": f"{left_indicator}_vs_{right_indicator}",
                    "source_family": "RE3D",
                    "source_best": left_indicator,
                    "source_run": int(left.iloc[0]["run"]),
                    "source_solution_id": int(left.iloc[0]["SolutionId"]),
                    "source_EP": float(left.iloc[0]["EP"]),
                    "source_HV": float(-left.iloc[0]["HVMinus"]),
                    "target_family": "RWA3D",
                    "target_best": right_indicator,
                    "target_run": int(right.iloc[0]["run"]),
                    "target_solution_id": int(right.iloc[0]["SolutionId"]),
                    "target_EP": float(right.iloc[0]["EP"]),
                    "target_HV": float(-right.iloc[0]["HVMinus"]),
                    "distance": float(distance),
                }
            )
    return pd.DataFrame(pairs)


def initial_medoids(distance_matrix: np.ndarray, k: int) -> np.ndarray:
    """Pick a simple set of initial medoids using centrality plus farthest-first."""
    n = distance_matrix.shape[0]
    if n <= k:
        return np.arange(n)

    medoids = [int(np.argmin(distance_matrix.mean(axis=1)))]
    while len(medoids) < k:
        current = distance_matrix[:, medoids].min(axis=1)
        current[medoids] = -1.0
        medoids.append(int(np.argmax(current)))
    return np.array(medoids, dtype=int)


def kmedoids(distance_matrix: np.ndarray, k: int, max_iter: int = 25) -> tuple[np.ndarray, np.ndarray]:
    """Run a small k-medoids routine on a precomputed distance matrix."""
    n = distance_matrix.shape[0]
    if n == 0:
        raise ValueError("Cannot cluster an empty distance matrix.")
    if n == 1:
        return np.array([0], dtype=int), np.array([0], dtype=int)

    k = min(k, n)
    medoids = initial_medoids(distance_matrix, k)

    for _ in range(max_iter):
        distances_to_medoids = distance_matrix[:, medoids]
        labels = distances_to_medoids.argmin(axis=1)
        new_medoids: list[int] = []

        for cluster_id in range(k):
            members = np.where(labels == cluster_id)[0]
            if len(members) == 0:
                remaining = [idx for idx in range(n) if idx not in new_medoids]
                if not remaining:
                    continue
                if new_medoids:
                    min_distance = distance_matrix[np.ix_(remaining, new_medoids)].min(axis=1)
                    new_medoids.append(int(remaining[int(np.argmax(min_distance))]))
                else:
                    new_medoids.append(int(remaining[0]))
                continue

            member_distances = distance_matrix[np.ix_(members, members)]
            member_mean = member_distances.mean(axis=1)
            new_medoids.append(int(members[int(np.argmin(member_mean))]))

        new_medoids_array = np.array(new_medoids, dtype=int)
        if np.array_equal(np.sort(new_medoids_array), np.sort(medoids)):
            medoids = new_medoids_array
            break
        medoids = new_medoids_array

    final_labels = distance_matrix[:, medoids].argmin(axis=1)
    return medoids, final_labels


def medoid_rows(
    family_df: pd.DataFrame,
    distance_matrix: np.ndarray,
    front_type: str,
    budget: int,
    scheme: str,
    k: int = MEDOID_K,
) -> pd.DataFrame:
    """Summarize k-medoids archetypes for one family/slice/scheme."""
    medoids, labels = kmedoids(distance_matrix, k=k)
    rows: list[dict[str, object]] = []

    for cluster_id, medoid_idx in enumerate(medoids):
        members = np.where(labels == cluster_id)[0]
        medoid_row = family_df.iloc[medoid_idx]
        if len(members) > 0:
            member_distances = distance_matrix[np.ix_(members, [medoid_idx])].ravel()
            mean_distance = float(member_distances.mean())
            median_distance = float(np.median(member_distances))
        else:
            mean_distance = 0.0
            median_distance = 0.0

        rows.append(
            {
                "front_type": front_type,
                "budget": budget,
                "scheme": scheme,
                "family": str(medoid_row["family"]),
                "cluster_id": int(cluster_id + 1),
                "cluster_size": int(len(members)),
                "cluster_share_pct": float(100.0 * len(members) / len(family_df)),
                "mean_distance_to_medoid": mean_distance,
                "median_distance_to_medoid": median_distance,
                "medoid_run": int(medoid_row["run"]),
                "medoid_solution_id": int(medoid_row["SolutionId"]),
                "medoid_config_id": str(medoid_row["config_id"]),
                "medoid_config_signature": str(medoid_row["config_signature"]),
                "medoid_EP": float(medoid_row["EP"]),
                "medoid_HV": float(-medoid_row["HVMinus"]),
            }
        )

    result = pd.DataFrame(rows).sort_values(
        ["family", "cluster_size", "mean_distance_to_medoid"],
        ascending=[True, False, True],
    )
    result["cluster_rank"] = result.groupby(["family"]).cumcount() + 1
    return result[
        [
            "front_type",
            "budget",
            "scheme",
            "family",
            "cluster_rank",
            "cluster_id",
            "cluster_size",
            "cluster_share_pct",
            "mean_distance_to_medoid",
            "median_distance_to_medoid",
            "medoid_run",
            "medoid_solution_id",
            "medoid_config_id",
            "medoid_config_signature",
            "medoid_EP",
            "medoid_HV",
        ]
    ].reset_index(drop=True)


def nearest_cross_matches(
    left: pd.DataFrame,
    right: pd.DataFrame,
    distance_matrix: np.ndarray,
    scheme: str,
    top_k: int = TOP_K,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Build nearest cross-benchmark matches and keep the closest pairs."""
    nearest_idx = distance_matrix.argmin(axis=1)
    nearest_dist = distance_matrix[np.arange(len(left)), nearest_idx]

    matches = pd.DataFrame(
        {
            "scheme": scheme,
            "source_family": left["family"].to_numpy(),
            "source_run": left["run"].to_numpy(),
            "source_solution_id": left["SolutionId"].to_numpy(),
            "source_config_id": left["config_id"].to_numpy(),
            "source_config_signature": left["config_signature"].to_numpy(),
            "source_EP": left["EP"].to_numpy(),
            "source_HV": (-left["HVMinus"]).to_numpy(),
            "target_family": right.iloc[nearest_idx]["family"].to_numpy(),
            "target_run": right.iloc[nearest_idx]["run"].to_numpy(),
            "target_solution_id": right.iloc[nearest_idx]["SolutionId"].to_numpy(),
            "target_config_id": right.iloc[nearest_idx]["config_id"].to_numpy(),
            "target_config_signature": right.iloc[nearest_idx]["config_signature"].to_numpy(),
            "target_EP": right.iloc[nearest_idx]["EP"].to_numpy(),
            "target_HV": (-right.iloc[nearest_idx]["HVMinus"]).to_numpy(),
            "distance": nearest_dist,
        }
    ).sort_values("distance", ascending=True)

    top_matches = matches.head(top_k).reset_index(drop=True)
    return matches.reset_index(drop=True), top_matches


def distance_distributions_figure(
    front_type: str,
    budget: int,
    unweighted: tuple[np.ndarray, np.ndarray, np.ndarray],
    weighted: tuple[np.ndarray, np.ndarray, np.ndarray],
) -> Path:
    """Plot within/between distributions for the two distance schemes."""
    setup_style()
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.6), sharey=True)

    panels = [
        ("Gower no ponderada", unweighted),
        ("Gower ponderada por consensus", weighted),
    ]
    colors = {
        "Within RE3D": "#1f77b4",
        "Within RWA3D": "#ff7f0e",
        "Between": "#2ca02c",
    }

    for ax, (title, (within_re3d, within_rwa, between)) in zip(axes, panels):
        ax.hist(
            within_re3d,
            bins=25,
            density=True,
            histtype="step",
            linewidth=1.8,
            color=colors["Within RE3D"],
            label=f"Within RE3D (media={within_re3d.mean():.3f})",
        )
        ax.hist(
            within_rwa,
            bins=25,
            density=True,
            histtype="step",
            linewidth=1.8,
            color=colors["Within RWA3D"],
            label=f"Within RWA3D (media={within_rwa.mean():.3f})",
        )
        ax.hist(
            between,
            bins=25,
            density=True,
            histtype="step",
            linewidth=1.8,
            color=colors["Between"],
            label=f"Between (media={between.mean():.3f})",
        )
        ax.set_title(title)
        ax.set_xlabel("Distancia")
        ax.set_ylabel("Densidad")
        ax.legend(frameon=False, loc="upper right")

    fig.suptitle(f"Distancia entre configuraciones finales: {FRONT_TYPE_LABELS[front_type]}, budget {budget}")
    return save_figure(
        fig,
        f"configuration_distance/budget_{budget}/configuration_distance_distribution_{front_type}_{budget}.png",
    )


def main() -> None:
    all_summaries: list[pd.DataFrame] = []
    all_best_rows: list[pd.DataFrame] = []
    all_medoid_rows: list[pd.DataFrame] = []

    for budget in BUDGETS:
        budget_table_dir = TABLES_ROOT / f"budget_{budget}"
        budget_figure_dir = FIGURES_ROOT / f"budget_{budget}"
        budget_table_dir.mkdir(parents=True, exist_ok=True)
        budget_figure_dir.mkdir(parents=True, exist_ok=True)

        for front_type in FRONT_TYPES:
            slice_data = load_final_slice(front_type, budget)
            pooled_ranges = normalization_ranges(slice_data.pooled, slice_data.config_columns)

            weights_df = consensus_mean_weights(front_type, budget, slice_data.config_columns)
            weights_path = budget_table_dir / f"configuration_distance_weights_{front_type}_{budget}.csv"
            weights_df.to_csv(weights_path, index=False)

            weight_map = dict(
                zip(weights_df["parameter_family"], weights_df["mean_weight"], strict=False)
            )

            re3d_unweighted = pairwise_gower(
                slice_data.re3d,
                slice_data.re3d,
                slice_data.config_columns,
                pooled_ranges,
            )
            rwa_unweighted = pairwise_gower(
                slice_data.rwa3d,
                slice_data.rwa3d,
                slice_data.config_columns,
                pooled_ranges,
            )
            between_unweighted = pairwise_gower(
                slice_data.re3d,
                slice_data.rwa3d,
                slice_data.config_columns,
                pooled_ranges,
            )

            re3d_weighted = pairwise_gower(
                slice_data.re3d,
                slice_data.re3d,
                slice_data.config_columns,
                pooled_ranges,
                weights=weight_map,
            )
            rwa_weighted = pairwise_gower(
                slice_data.rwa3d,
                slice_data.rwa3d,
                slice_data.config_columns,
                pooled_ranges,
                weights=weight_map,
            )
            between_weighted = pairwise_gower(
                slice_data.re3d,
                slice_data.rwa3d,
                slice_data.config_columns,
                pooled_ranges,
                weights=weight_map,
            )

            summary_df = pd.DataFrame(
                [
                    distance_summary_row(
                        front_type,
                        budget,
                        "gower_unweighted",
                        re3d_unweighted,
                        rwa_unweighted,
                        between_unweighted,
                    ),
                    distance_summary_row(
                        front_type,
                        budget,
                        "gower_consensus_weighted",
                        re3d_weighted,
                        rwa_weighted,
                        between_weighted,
                    ),
                ]
            )
            summary_path = budget_table_dir / f"configuration_distance_summary_{front_type}_{budget}.csv"
            summary_df.to_csv(summary_path, index=False)
            all_summaries.append(summary_df)

            best_distance_df = pd.concat(
                [
                    best_config_distance_rows(
                        slice_data,
                        pooled_ranges,
                        "gower_unweighted",
                        None,
                    ),
                    best_config_distance_rows(
                        slice_data,
                        pooled_ranges,
                        "gower_consensus_weighted",
                        weight_map,
                    ),
                ],
                ignore_index=True,
            )
            best_distance_path = budget_table_dir / f"configuration_distance_best_configs_{front_type}_{budget}.csv"
            best_distance_df.to_csv(best_distance_path, index=False)
            all_best_rows.append(best_distance_df)

            medoid_df = pd.concat(
                [
                    medoid_rows(
                        slice_data.re3d,
                        re3d_unweighted,
                        front_type,
                        budget,
                        "gower_unweighted",
                    ),
                    medoid_rows(
                        slice_data.rwa3d,
                        rwa_unweighted,
                        front_type,
                        budget,
                        "gower_unweighted",
                    ),
                    medoid_rows(
                        slice_data.re3d,
                        re3d_weighted,
                        front_type,
                        budget,
                        "gower_consensus_weighted",
                    ),
                    medoid_rows(
                        slice_data.rwa3d,
                        rwa_weighted,
                        front_type,
                        budget,
                        "gower_consensus_weighted",
                    ),
                ],
                ignore_index=True,
            )
            medoid_path = budget_table_dir / f"configuration_distance_medoids_{front_type}_{budget}.csv"
            medoid_df.to_csv(medoid_path, index=False)
            all_medoid_rows.append(medoid_df)

            unweighted_matches_a, unweighted_top_a = nearest_cross_matches(
                slice_data.re3d,
                slice_data.rwa3d,
                between_unweighted,
                "gower_unweighted",
            )
            unweighted_matches_b, unweighted_top_b = nearest_cross_matches(
                slice_data.rwa3d,
                slice_data.re3d,
                between_unweighted.T,
                "gower_unweighted",
            )
            weighted_matches_a, weighted_top_a = nearest_cross_matches(
                slice_data.re3d,
                slice_data.rwa3d,
                between_weighted,
                "gower_consensus_weighted",
            )
            weighted_matches_b, weighted_top_b = nearest_cross_matches(
                slice_data.rwa3d,
                slice_data.re3d,
                between_weighted.T,
                "gower_consensus_weighted",
            )

            nearest_all = pd.concat(
                [
                    unweighted_matches_a,
                    unweighted_matches_b,
                    weighted_matches_a,
                    weighted_matches_b,
                ],
                ignore_index=True,
            )
            nearest_top = pd.concat(
                [
                    unweighted_top_a,
                    unweighted_top_b,
                    weighted_top_a,
                    weighted_top_b,
                ],
                ignore_index=True,
            )

            nearest_all_path = budget_table_dir / f"configuration_distance_nearest_cross_{front_type}_{budget}.csv"
            nearest_all.to_csv(nearest_all_path, index=False)
            nearest_top_path = budget_table_dir / f"configuration_distance_nearest_cross_top{TOP_K}_{front_type}_{budget}.csv"
            nearest_top.to_csv(nearest_top_path, index=False)

            figure_path = distance_distributions_figure(
                front_type,
                budget,
                (
                    upper_triangle_values(re3d_unweighted),
                    upper_triangle_values(rwa_unweighted),
                    between_unweighted.ravel(),
                ),
                (
                    upper_triangle_values(re3d_weighted),
                    upper_triangle_values(rwa_weighted),
                    between_weighted.ravel(),
                ),
            )

            print(f"Saved: {weights_path}")
            print(f"Saved: {summary_path}")
            print(f"Saved: {best_distance_path}")
            print(f"Saved: {medoid_path}")
            print(f"Saved: {nearest_all_path}")
            print(f"Saved: {nearest_top_path}")
            print(f"Saved: {figure_path}")

    all_summary_df = pd.concat(all_summaries, ignore_index=True).sort_values(
        ["budget", "front_type", "scheme"]
    )
    all_summary_path = TABLES_ROOT / "configuration_distance_summary_all.csv"
    all_summary_df.to_csv(all_summary_path, index=False)
    all_best_df = pd.concat(all_best_rows, ignore_index=True).sort_values(
        ["budget", "front_type", "scheme", "pair"]
    )
    all_best_path = TABLES_ROOT / "configuration_distance_best_configs_all.csv"
    all_best_df.to_csv(all_best_path, index=False)
    all_medoids_df = pd.concat(all_medoid_rows, ignore_index=True).sort_values(
        ["budget", "front_type", "scheme", "family", "cluster_rank"]
    )
    all_medoids_path = TABLES_ROOT / "configuration_distance_medoids_all.csv"
    all_medoids_df.to_csv(all_medoids_path, index=False)
    print(f"Saved: {all_summary_path}")
    print(f"Saved: {all_best_path}")
    print(f"Saved: {all_medoids_path}")


if __name__ == "__main__":
    main()
