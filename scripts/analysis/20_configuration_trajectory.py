"""Track how incumbent configurations stabilize during meta-optimization."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import (
    CHECKPOINT_FRACTIONS,
    JOINT_VIEW,
    active_configuration_columns,
    add_joint_scores,
    checkpoint_targets,
    elite_subset,
    iter_slices,
    medoid_index,
    mixed_normalization_ranges,
    pairwise_gower,
    parameter_kind,
    prioritized_parameters,
)
from data_loader import get_final_configs, load_all_runs_with_config

FIGURE_ROOT = Path("configuration_trajectory")
TABLE_ROOT = TABLES_DIR / "configuration_trajectory"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)

NUMERIC_STABILITY_TOLERANCE_SHARE = 0.05


def load_slice(family: str, front_type: str, budget: int) -> pd.DataFrame:
    """Load a full configuration trace for one slice."""
    traced = load_all_runs_with_config(family, front_type, budget).copy()
    traced["trace_row_id"] = np.arange(len(traced))
    return add_joint_scores(traced)


def final_elite_region(
    traced_df: pd.DataFrame,
) -> tuple[pd.DataFrame, pd.Series, dict[str, float], list[str], float]:
    """Build the final elite medoid and its radius."""
    final_df = add_joint_scores(get_final_configs(traced_df)).copy()
    config_columns = active_configuration_columns(final_df)
    pooled_ranges = mixed_normalization_ranges(final_df, config_columns)
    elite_final_df = elite_subset(final_df, JOINT_VIEW, 0.10).copy().reset_index(drop=True)
    elite_matrix = pairwise_gower(
        elite_final_df,
        elite_final_df,
        config_columns,
        pooled_ranges,
    )
    medoid_idx = medoid_index(elite_matrix)
    medoid_row = elite_final_df.iloc[medoid_idx]
    elite_radius = float(np.quantile(elite_matrix[:, medoid_idx], 0.90)) if len(elite_final_df) > 1 else 0.0
    return elite_final_df, medoid_row, pooled_ranges, config_columns, elite_radius


def best_rows_by_evaluation(run_df: pd.DataFrame) -> pd.DataFrame:
    """Keep the best row at each evaluation according to joint score."""
    ordered = run_df.sort_values(["Evaluation", "joint_score", "EP", "HVMinus", "SolutionId"])
    return ordered.groupby("Evaluation", as_index=False).head(1).reset_index(drop=True)


def incumbent_trace(best_eval_df: pd.DataFrame) -> pd.DataFrame:
    """Carry forward the best-so-far configuration over evaluations."""
    best_score = None
    incumbent_rows: list[pd.Series] = []
    incumbent_source_eval = None

    for _, row in best_eval_df.sort_values("Evaluation").iterrows():
        score = float(row["joint_score"])
        if best_score is None or score < best_score:
            best_score = score
            incumbent_source_eval = int(row["Evaluation"])
            incumbent = row.copy()
        checkpoint_row = incumbent.copy()
        checkpoint_row["Evaluation"] = int(row["Evaluation"])
        checkpoint_row["incumbent_found_at"] = incumbent_source_eval
        incumbent_rows.append(checkpoint_row)

    return pd.DataFrame(incumbent_rows).reset_index(drop=True)


def checkpoint_trace(incumbent_df: pd.DataFrame, checkpoints: list[int]) -> pd.DataFrame:
    """Select incumbent rows at fixed checkpoints."""
    checkpoint_rows: list[pd.Series] = []
    ordered = incumbent_df.sort_values("Evaluation")
    for checkpoint in checkpoints:
        row = ordered.loc[ordered["Evaluation"] <= checkpoint].tail(1)
        if row.empty:
            row = ordered.head(1)
        selected = row.iloc[0].copy()
        selected["checkpoint_eval"] = int(checkpoint)
        selected["checkpoint_fraction"] = checkpoint / float(checkpoints[-1])
        checkpoint_rows.append(selected)
    return pd.DataFrame(checkpoint_rows).reset_index(drop=True)


def parameter_stabilization_eval(
    checkpoint_df: pd.DataFrame,
    parameter: str,
    observed_range: float | None,
) -> int:
    """Return the earliest checkpoint where a parameter remains at its final value."""
    final_value = checkpoint_df.iloc[-1][parameter]
    tail = checkpoint_df[[parameter, "checkpoint_eval"]].reset_index(drop=True)

    if parameter_kind(parameter) == "categorical":
        final_label = "<NA>" if pd.isna(final_value) else str(final_value)
        for index in range(len(tail)):
            labels = tail.loc[index:, parameter].map(lambda value: "<NA>" if pd.isna(value) else str(value))
            if (labels == final_label).all():
                return int(tail.loc[index, "checkpoint_eval"])
        return int(tail.iloc[-1]["checkpoint_eval"])

    tolerance = max(1e-9, float((observed_range or 0.0) * NUMERIC_STABILITY_TOLERANCE_SHARE))
    for index in range(len(tail)):
        values = pd.to_numeric(tail.loc[index:, parameter], errors="coerce")
        if pd.isna(final_value):
            stable = values.isna().all()
        else:
            stable = values.map(lambda value: pd.notna(value) and abs(float(value) - float(final_value)) <= tolerance).all()
        if stable:
            return int(tail.loc[index, "checkpoint_eval"])
    return int(tail.iloc[-1]["checkpoint_eval"])


def distance_to_medoid(
    row: pd.Series,
    medoid_row: pd.Series,
    config_columns: list[str],
    pooled_ranges: dict[str, float],
) -> float:
    """Compute Gower distance from one incumbent row to the final elite medoid."""
    left = pd.DataFrame([row])[config_columns]
    right = pd.DataFrame([medoid_row])[config_columns]
    return float(pairwise_gower(left, right, config_columns, pooled_ranges)[0, 0])


def plot_slice_trajectory(
    family: str,
    front_type: str,
    budget: int,
    checkpoint_distance_df: pd.DataFrame,
    stability_df: pd.DataFrame,
    parameters_for_figure: list[str],
) -> Path:
    """Create a distance-plus-stabilization figure for one slice."""
    fig = plt.figure(figsize=(13, 9))
    grid = fig.add_gridspec(2, 1, height_ratios=[1.0, 1.5])
    ax_distance = fig.add_subplot(grid[0, 0])
    ax_heatmap = fig.add_subplot(grid[1, 0])

    checkpoint_summary = (
        checkpoint_distance_df.groupby("checkpoint_eval")["distance_to_final_elite_medoid"]
        .agg(["median", lambda values: values.quantile(0.25), lambda values: values.quantile(0.75)])
        .reset_index()
    )
    checkpoint_summary.columns = ["checkpoint_eval", "median", "q25", "q75"]
    ax_distance.plot(
        checkpoint_summary["checkpoint_eval"],
        checkpoint_summary["median"],
        color="#1b9e77",
        linewidth=2.0,
    )
    ax_distance.fill_between(
        checkpoint_summary["checkpoint_eval"],
        checkpoint_summary["q25"],
        checkpoint_summary["q75"],
        color="#1b9e77",
        alpha=0.2,
    )
    ax_distance.set_title("Distancia del incumbente al medoid élite final", fontsize=11)
    ax_distance.set_xlabel("Meta-evaluación")
    ax_distance.set_ylabel("Distancia Gower")

    selected = [
        parameter
        for parameter in parameters_for_figure
        if parameter in stability_df["parameter"].unique()
    ][:10]
    if not selected:
        selected = stability_df["parameter"].drop_duplicates().head(10).tolist()

    checkpoint_order = sorted(stability_df["checkpoint_eval"].unique())
    matrix_rows: list[list[float]] = []
    for parameter in selected:
        sub = stability_df.loc[stability_df["parameter"] == parameter]
        shares = []
        for checkpoint in checkpoint_order:
            share = sub.loc[sub["checkpoint_eval"] == checkpoint, "stable_by_checkpoint_share"]
            shares.append(float(share.iloc[0]) if not share.empty else 0.0)
        matrix_rows.append(shares)

    heatmap = ax_heatmap.imshow(np.array(matrix_rows), aspect="auto", cmap="YlGn", vmin=0.0, vmax=1.0)
    ax_heatmap.set_yticks(np.arange(len(selected)))
    ax_heatmap.set_yticklabels(selected)
    ax_heatmap.set_xticks(np.arange(len(checkpoint_order)))
    ax_heatmap.set_xticklabels([str(value) for value in checkpoint_order])
    ax_heatmap.set_xlabel("Checkpoint")
    ax_heatmap.set_title("Fracción de runs estabilizados por parámetro", fontsize=11)
    fig.colorbar(heatmap, ax=ax_heatmap, fraction=0.025, pad=0.02, label="Share")

    fig.suptitle(
        f"Trayectoria estructural | {family} | {FRONT_TYPE_LABELS[front_type]} | budget {budget}",
        fontsize=13,
        fontweight="bold",
    )
    fig.tight_layout()
    filename = f"trajectory_{family}_{front_type}_{budget}.png"
    return save_figure(fig, str(FIGURE_ROOT / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()

    all_parameter_rows: list[dict[str, object]] = []
    all_entry_rows: list[dict[str, object]] = []
    all_distance_rows: list[dict[str, object]] = []

    for family, front_type, budget in iter_slices():
        print(f"Processing {family} | {front_type} | budget {budget} ...")
        traced_df = load_slice(family, front_type, budget)
        elite_final_df, medoid_row, pooled_ranges, config_columns, elite_radius = final_elite_region(traced_df)
        checkpoints = checkpoint_targets(traced_df["Evaluation"], CHECKPOINT_FRACTIONS)
        final_eval = checkpoints[-1]

        observed_ranges = {
            parameter: float(
                pd.to_numeric(elite_final_df[parameter], errors="coerce").max()
                - pd.to_numeric(elite_final_df[parameter], errors="coerce").min()
            )
            if parameter_kind(parameter) == "numerical"
            else None
            for parameter in config_columns
        }

        for run, run_df in traced_df.groupby("run", sort=True):
            best_eval_df = best_rows_by_evaluation(run_df)
            incumbent_df = incumbent_trace(best_eval_df)
            checkpoint_df = checkpoint_trace(incumbent_df, checkpoints)

            entry_eval = final_eval
            final_distance = None
            for _, row in checkpoint_df.iterrows():
                distance = distance_to_medoid(row, medoid_row, config_columns, pooled_ranges)
                final_distance = distance
                all_distance_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "run": int(run),
                        "checkpoint_eval": int(row["checkpoint_eval"]),
                        "checkpoint_fraction": float(row["checkpoint_fraction"]),
                        "incumbent_found_at": int(row["incumbent_found_at"]),
                        "distance_to_final_elite_medoid": distance,
                    }
                )
                if distance <= elite_radius and entry_eval == final_eval:
                    entry_eval = int(row["checkpoint_eval"])

            all_entry_rows.append(
                {
                    "family": family,
                    "front_type": front_type,
                    "budget": budget,
                    "run": int(run),
                    "entry_eval": int(entry_eval),
                    "entry_pct_meta_budget": round(100.0 * entry_eval / final_eval, 1),
                    "elite_radius": elite_radius,
                    "final_distance_to_medoid": final_distance,
                }
            )

            for parameter in config_columns:
                stabilization_eval = parameter_stabilization_eval(
                    checkpoint_df,
                    parameter,
                    observed_ranges.get(parameter),
                )
                all_parameter_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "run": int(run),
                        "parameter": parameter,
                        "parameter_kind": parameter_kind(parameter),
                        "stabilization_eval": int(stabilization_eval),
                        "stabilization_pct_meta_budget": round(100.0 * stabilization_eval / final_eval, 1),
                    }
                )

        slice_parameter_df = pd.DataFrame(all_parameter_rows)
        slice_parameter_df = slice_parameter_df.loc[
            (slice_parameter_df["family"] == family)
            & (slice_parameter_df["front_type"] == front_type)
            & (slice_parameter_df["budget"] == budget)
        ].copy()
        checkpoint_grid_rows: list[dict[str, object]] = []
        for parameter, group in slice_parameter_df.groupby("parameter"):
            for checkpoint in checkpoints:
                checkpoint_grid_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "parameter": parameter,
                        "checkpoint_eval": checkpoint,
                        "stable_by_checkpoint_share": float(
                            (group["stabilization_eval"] <= checkpoint).mean()
                        ),
                    }
                )
        slice_stability_grid_df = pd.DataFrame(checkpoint_grid_rows)
        slice_distance_df = pd.DataFrame(all_distance_rows).loc[
            (pd.DataFrame(all_distance_rows)["family"] == family)
            & (pd.DataFrame(all_distance_rows)["front_type"] == front_type)
            & (pd.DataFrame(all_distance_rows)["budget"] == budget)
        ].copy()
        figure_path = plot_slice_trajectory(
            family,
            front_type,
            budget,
            slice_distance_df,
            slice_stability_grid_df,
            prioritized_parameters(front_type, budget, config_columns, top_k=10),
        )
        print(f"  Saved: {figure_path}")

    parameter_df = pd.DataFrame(all_parameter_rows).sort_values(
        ["budget", "front_type", "family", "parameter", "run"]
    )
    entry_df = pd.DataFrame(all_entry_rows).sort_values(
        ["budget", "front_type", "family", "run"]
    )
    distance_df = pd.DataFrame(all_distance_rows).sort_values(
        ["budget", "front_type", "family", "run", "checkpoint_eval"]
    )

    parameter_summary_df = (
        parameter_df.groupby(["family", "front_type", "budget", "parameter", "parameter_kind"], as_index=False)
        .agg(
            stabilization_eval_median=("stabilization_eval", "median"),
            stabilization_eval_min=("stabilization_eval", "min"),
            stabilization_eval_max=("stabilization_eval", "max"),
            stabilization_pct_median=("stabilization_pct_meta_budget", "median"),
            stable_by_50_pct=("stabilization_pct_meta_budget", lambda values: float((values <= 50.0).mean())),
            stable_by_75_pct=("stabilization_pct_meta_budget", lambda values: float((values <= 75.0).mean())),
            stable_by_100_pct=("stabilization_pct_meta_budget", lambda values: float((values <= 100.0).mean())),
        )
        .sort_values(["budget", "front_type", "family", "stabilization_pct_median", "parameter"])
    )
    entry_summary_df = (
        entry_df.groupby(["family", "front_type", "budget"], as_index=False)
        .agg(
            entry_eval_median=("entry_eval", "median"),
            entry_eval_min=("entry_eval", "min"),
            entry_eval_max=("entry_eval", "max"),
            entry_pct_median=("entry_pct_meta_budget", "median"),
            median_final_distance_to_medoid=("final_distance_to_medoid", "median"),
        )
        .sort_values(["budget", "front_type", "family"])
    )

    parameter_df.to_csv(TABLE_ROOT / "trajectory_parameter_stability_all.csv", index=False)
    parameter_summary_df.to_csv(TABLE_ROOT / "trajectory_parameter_stability_summary_all.csv", index=False)
    entry_df.to_csv(TABLE_ROOT / "trajectory_medoid_entry_all.csv", index=False)
    entry_summary_df.to_csv(TABLE_ROOT / "trajectory_medoid_entry_summary_all.csv", index=False)
    distance_df.to_csv(TABLE_ROOT / "trajectory_checkpoint_distances_all.csv", index=False)

    for budget in sorted(parameter_df["budget"].unique()):
        budget_dir = TABLE_ROOT / f"budget_{int(budget)}"
        budget_dir.mkdir(parents=True, exist_ok=True)
        parameter_df.loc[parameter_df["budget"] == budget].to_csv(
            budget_dir / f"trajectory_parameter_stability_{int(budget)}.csv",
            index=False,
        )
        parameter_summary_df.loc[parameter_summary_df["budget"] == budget].to_csv(
            budget_dir / f"trajectory_parameter_stability_summary_{int(budget)}.csv",
            index=False,
        )
        entry_df.loc[entry_df["budget"] == budget].to_csv(
            budget_dir / f"trajectory_medoid_entry_{int(budget)}.csv",
            index=False,
        )
        distance_df.loc[distance_df["budget"] == budget].to_csv(
            budget_dir / f"trajectory_checkpoint_distances_{int(budget)}.csv",
            index=False,
        )

    print(f"Saved: {TABLE_ROOT / 'trajectory_parameter_stability_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'trajectory_parameter_stability_summary_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'trajectory_medoid_entry_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'trajectory_medoid_entry_summary_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'trajectory_checkpoint_distances_all.csv'}")


if __name__ == "__main__":
    main()
