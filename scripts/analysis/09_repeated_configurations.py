"""Most repeated final configurations by benchmark and consensus."""

from __future__ import annotations

import numpy as np
import pandas as pd

from config import BUDGETS, FRONT_TYPES, TABLES_DIR
from data_loader import (
    build_config_signature,
    get_configuration_columns,
    get_final_configs,
    load_all_runs_with_config,
)

FAMILIES = ["RE3D", "RWA3D"]
TOP_K = 10
OUTPUT_ROOT = TABLES_DIR / "repeated_configurations"
OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def repeated_configs_for_family(
    family: str, front_type: str, budget: int
) -> pd.DataFrame:
    """Count repeated final configurations for one benchmark slice."""
    final_df = get_final_configs(
        load_all_runs_with_config(family, front_type, budget)
    ).copy()
    config_columns = get_configuration_columns(final_df)
    final_df["config_signature"] = build_config_signature(final_df)

    summary = (
        final_df.groupby("config_signature", as_index=False)
        .agg(
            rows=("config_signature", "size"),
            runs=("run", "nunique"),
            ep_median=("EP", "median"),
            hv_median=("HVMinus", "median"),
        )
        .sort_values(
            ["rows", "runs", "ep_median", "hv_median"],
            ascending=[False, False, True, True],
        )
        .reset_index(drop=True)
    )
    summary["rank"] = np.arange(1, len(summary) + 1)
    summary["row_share_pct"] = 100.0 * summary["rows"] / len(final_df)
    summary["run_share_pct"] = 100.0 * summary["runs"] / final_df["run"].nunique()
    summary["scope"] = family
    summary["front_type"] = front_type
    summary["budget"] = budget
    best_ep_signature = summary.sort_values(
        ["ep_median", "hv_median", "rows", "runs"],
        ascending=[True, True, False, False],
    ).iloc[0]["config_signature"]
    best_hv_signature = summary.sort_values(
        ["hv_median", "ep_median", "rows", "runs"],
        ascending=[True, True, False, False],
    ).iloc[0]["config_signature"]
    summary["is_best_EP"] = summary["config_signature"].eq(best_ep_signature)
    summary["is_best_HV"] = summary["config_signature"].eq(best_hv_signature)

    examples = (
        final_df.sort_values(["config_signature", "run", "SolutionId"])
        .drop_duplicates("config_signature")
        [["config_signature"] + config_columns]
    )
    return summary.merge(examples, on="config_signature", how="left")


def consensus_configs(
    re3d_df: pd.DataFrame, rwa_df: pd.DataFrame, front_type: str, budget: int
) -> pd.DataFrame:
    """Keep only exact configurations repeated in both benchmarks."""
    merge_columns = ["config_signature"] + get_configuration_columns(re3d_df)
    left = re3d_df[merge_columns + ["rows", "runs", "row_share_pct", "run_share_pct", "ep_median", "hv_median"]]
    right = rwa_df[merge_columns + ["rows", "runs", "row_share_pct", "run_share_pct", "ep_median", "hv_median"]]
    shared = left.merge(
        right,
        on=merge_columns,
        how="inner",
        suffixes=("_RE3D", "_RWA3D"),
    )

    if shared.empty:
        columns = [
            "front_type",
            "budget",
            "consensus_rank",
            "config_signature",
            "consensus_rows",
            "consensus_runs",
            "consensus_row_share_pct",
            "consensus_run_share_pct",
            "mean_row_share_pct",
            "mean_run_share_pct",
            "rows_RE3D",
            "rows_RWA3D",
            "runs_RE3D",
            "runs_RWA3D",
            "row_share_pct_RE3D",
            "row_share_pct_RWA3D",
            "run_share_pct_RE3D",
            "run_share_pct_RWA3D",
            "ep_median_RE3D",
            "ep_median_RWA3D",
            "hv_median_RE3D",
            "hv_median_RWA3D",
        ] + get_configuration_columns(re3d_df)
        return pd.DataFrame(columns=columns)

    shared["consensus_rows"] = shared[["rows_RE3D", "rows_RWA3D"]].min(axis=1)
    shared["consensus_runs"] = shared[["runs_RE3D", "runs_RWA3D"]].min(axis=1)
    shared["consensus_row_share_pct"] = shared[
        ["row_share_pct_RE3D", "row_share_pct_RWA3D"]
    ].min(axis=1)
    shared["consensus_run_share_pct"] = shared[
        ["run_share_pct_RE3D", "run_share_pct_RWA3D"]
    ].min(axis=1)
    shared["mean_row_share_pct"] = shared[
        ["row_share_pct_RE3D", "row_share_pct_RWA3D"]
    ].mean(axis=1)
    shared["mean_run_share_pct"] = shared[
        ["run_share_pct_RE3D", "run_share_pct_RWA3D"]
    ].mean(axis=1)
    shared["front_type"] = front_type
    shared["budget"] = budget

    shared = shared.sort_values(
        [
            "consensus_row_share_pct",
            "consensus_run_share_pct",
            "mean_row_share_pct",
            "mean_run_share_pct",
            "ep_median_RE3D",
            "ep_median_RWA3D",
        ],
        ascending=[False, False, False, False, True, True],
    ).reset_index(drop=True)
    shared["consensus_rank"] = np.arange(1, len(shared) + 1)

    ordered_columns = [
        "front_type",
        "budget",
        "consensus_rank",
        "config_signature",
        "consensus_rows",
        "consensus_runs",
        "consensus_row_share_pct",
        "consensus_run_share_pct",
        "mean_row_share_pct",
        "mean_run_share_pct",
        "rows_RE3D",
        "rows_RWA3D",
        "runs_RE3D",
        "runs_RWA3D",
        "row_share_pct_RE3D",
        "row_share_pct_RWA3D",
        "run_share_pct_RE3D",
        "run_share_pct_RWA3D",
        "ep_median_RE3D",
        "ep_median_RWA3D",
        "hv_median_RE3D",
        "hv_median_RWA3D",
    ] + get_configuration_columns(re3d_df)
    return shared[ordered_columns]


def main() -> None:
    for budget in BUDGETS:
        budget_dir = OUTPUT_ROOT / f"budget_{budget}"
        budget_dir.mkdir(parents=True, exist_ok=True)

        separate_frames: list[pd.DataFrame] = []
        consensus_frames: list[pd.DataFrame] = []

        for front_type in FRONT_TYPES:
            repeated_by_family = {
                family: repeated_configs_for_family(family, front_type, budget)
                for family in FAMILIES
            }

            for family, summary_df in repeated_by_family.items():
                separate_frames.append(summary_df)
                family_path = budget_dir / f"repeated_configs_{family}_{front_type}_{budget}.csv"
                summary_df.to_csv(family_path, index=False)
                top_path = budget_dir / f"repeated_configs_top{TOP_K}_{family}_{front_type}_{budget}.csv"
                summary_df.head(TOP_K).to_csv(top_path, index=False)
                print(f"Saved: {family_path}")
                print(f"Saved: {top_path}")

            consensus_df = consensus_configs(
                repeated_by_family["RE3D"],
                repeated_by_family["RWA3D"],
                front_type,
                budget,
            )
            consensus_frames.append(consensus_df)
            consensus_path = budget_dir / f"repeated_configs_consensus_{front_type}_{budget}.csv"
            consensus_df.to_csv(consensus_path, index=False)
            consensus_top_path = (
                budget_dir
                / f"repeated_configs_consensus_top{TOP_K}_{front_type}_{budget}.csv"
            )
            consensus_df.head(TOP_K).to_csv(consensus_top_path, index=False)
            print(f"Saved: {consensus_path}")
            print(f"Saved: {consensus_top_path}")

        separate_summary = pd.concat(separate_frames, ignore_index=True)
        separate_summary_path = budget_dir / f"repeated_configs_summary_{budget}.csv"
        separate_summary.to_csv(separate_summary_path, index=False)

        consensus_summary = pd.concat(consensus_frames, ignore_index=True)
        consensus_summary_path = budget_dir / f"repeated_configs_consensus_summary_{budget}.csv"
        consensus_summary.to_csv(consensus_summary_path, index=False)

        print(f"Saved: {separate_summary_path}")
        print(f"Saved: {consensus_summary_path}")


if __name__ == "__main__":
    main()
