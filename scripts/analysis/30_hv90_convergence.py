"""Convergence to 90% HV improvement and early-vs-late configuration comparison."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import FRONT_TYPE_COLORS, FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import (
    REPRESENTATIVE_CATEGORICALS,
    REPRESENTATIVE_NUMERICALS,
    mode_value,
    report_table_path,
)
from data_loader import (
    best_config_per_evaluation,
    get_final_best_configs,
    load_all_runs_with_config,
)

TABLE_ROOT = TABLES_DIR / "hv90_convergence"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("hv90_convergence")


def run_hv90_row(run_df: pd.DataFrame) -> tuple[dict[str, object], pd.Series]:
    best_eval = best_config_per_evaluation(run_df).sort_values("Evaluation").reset_index(drop=True)
    best_eval["HV"] = -best_eval["HVMinus"]
    best_eval["running_best_HV"] = best_eval["HV"].cummax()

    hv_min = float(best_eval["running_best_HV"].iloc[0])
    hv_max = float(best_eval["running_best_HV"].iloc[-1])
    threshold = hv_min + 0.90 * (hv_max - hv_min)
    reached = best_eval.loc[best_eval["running_best_HV"] >= threshold].head(1)
    early_row = reached.iloc[0] if not reached.empty else best_eval.iloc[-1]

    row = {
        "run": int(best_eval["run"].iloc[0]),
        "eval_90": int(early_row["Evaluation"]),
        "hv_start": hv_min,
        "hv_end": hv_max,
        "hv_threshold_90": threshold,
        "early_hv": float(early_row["running_best_HV"]),
    }
    return row, early_row


def load_budget7000_late_configs(family: str) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for front_type in ["referenceFronts", "extremePointsFronts"]:
        final_best = get_final_best_configs(load_all_runs_with_config(family, front_type, 7000)).copy()
        final_best["family"] = family
        final_best["front_type"] = front_type
        final_best["budget"] = 7000
        frames.append(final_best)
    return pd.concat(frames, ignore_index=True)


def parameter_comparison(
    family: str, early_df: pd.DataFrame, late_df: pd.DataFrame
) -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for parameter in [*REPRESENTATIVE_CATEGORICALS, *REPRESENTATIVE_NUMERICALS]:
        if parameter not in early_df.columns and parameter not in late_df.columns:
            continue

        if parameter in REPRESENTATIVE_CATEGORICALS:
            early_mode = mode_value(early_df[parameter]) if parameter in early_df.columns else np.nan
            late_mode = mode_value(late_df[parameter]) if parameter in late_df.columns else np.nan
            rows.append(
                {
                    "family": family,
                    "parameter": parameter,
                    "parameter_kind": "categorical",
                    "early_value": early_mode,
                    "late_value": late_mode,
                    "abs_pct_diff": np.nan,
                    "consistent": bool(str(early_mode) == str(late_mode)),
                }
            )
            continue

        early_values = pd.to_numeric(early_df.get(parameter, pd.Series(dtype=float)), errors="coerce").dropna()
        late_values = pd.to_numeric(late_df.get(parameter, pd.Series(dtype=float)), errors="coerce").dropna()
        early_median = float(early_values.median()) if not early_values.empty else np.nan
        late_median = float(late_values.median()) if not late_values.empty else np.nan
        abs_pct_diff = (
            100.0 * abs(early_median - late_median) / abs(late_median)
            if np.isfinite(late_median) and late_median != 0.0 and np.isfinite(early_median)
            else np.nan
        )
        rows.append(
            {
                "family": family,
                "parameter": parameter,
                "parameter_kind": "numerical",
                "early_value": early_median,
                "late_value": late_median,
                "abs_pct_diff": abs_pct_diff,
                "consistent": bool(np.isfinite(abs_pct_diff) and abs_pct_diff <= 10.0),
            }
        )
    return pd.DataFrame(rows)


def plot_cdf(cumulative_df: pd.DataFrame) -> Path:
    fig, axes = plt.subplots(1, 2, figsize=(14, 5), sharey=True)
    fig.suptitle("CDF of eval_90", fontsize=13, fontweight="bold")

    for ax, family in zip(axes, ["RE3D", "RWA3D"], strict=False):
        family_df = cumulative_df.loc[cumulative_df["family"] == family].copy()
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            sub = family_df.loc[family_df["front_type"] == front_type]
            ax.plot(
                sub["evaluation"],
                sub["share_converged_pct"],
                color=FRONT_TYPE_COLORS[front_type],
                linewidth=2.0,
                label=FRONT_TYPE_LABELS[front_type],
            )
        pooled = (
            family_df.groupby("evaluation", as_index=False)["share_converged_pct"]
            .mean()
            .sort_values("evaluation")
        )
        ax.plot(pooled["evaluation"], pooled["share_converged_pct"], color="#444444", linestyle="--", linewidth=1.5, label="Mean")
        ax.set_title(family)
        ax.set_xlabel("Meta-evaluation")
        ax.set_ylabel("Runs converged (%)")
        ax.legend(frameon=False, fontsize=9)

    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / "hv90_cdf.png"))


def main() -> None:
    setup_style()

    eval_rows: list[dict[str, object]] = []
    early_config_rows: list[pd.Series] = []

    for family in ["RE3D", "RWA3D"]:
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            for budget in [1000, 3000, 5000, 7000]:
                traced_df = load_all_runs_with_config(family, front_type, budget).copy()
                for run, run_df in traced_df.groupby("run", sort=True):
                    row, early_row = run_hv90_row(run_df)
                    row["family"] = family
                    row["front_type"] = front_type
                    row["budget"] = budget
                    eval_rows.append(row)

                    tagged_early = early_row.copy()
                    tagged_early["family"] = family
                    tagged_early["front_type"] = front_type
                    tagged_early["budget"] = budget
                    early_config_rows.append(tagged_early)

    eval_df = pd.DataFrame(eval_rows).sort_values(["family", "front_type", "budget", "run"])
    early_df = pd.DataFrame(early_config_rows).reset_index(drop=True)

    percentile_rows: list[dict[str, object]] = []
    cumulative_rows: list[dict[str, object]] = []
    threshold_rows: list[dict[str, object]] = []
    comparison_rows: list[pd.DataFrame] = []

    for family in ["RE3D", "RWA3D"]:
        family_eval_df = eval_df.loc[eval_df["family"] == family].copy()
        family_early_df = early_df.loc[early_df["family"] == family].copy()
        family_late_df = load_budget7000_late_configs(family)

        percentile_rows.append(
            {
                "family": family,
                "p10": float(family_eval_df["eval_90"].quantile(0.10)),
                "p25": float(family_eval_df["eval_90"].quantile(0.25)),
                "median": float(family_eval_df["eval_90"].median()),
                "p75": float(family_eval_df["eval_90"].quantile(0.75)),
                "p90": float(family_eval_df["eval_90"].quantile(0.90)),
            }
        )

        for front_type, sub in family_eval_df.groupby("front_type", sort=True):
            for evaluation in sorted(sub["eval_90"].unique()):
                cumulative_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "evaluation": int(evaluation),
                        "share_converged_pct": float(100.0 * (sub["eval_90"] <= evaluation).mean()),
                    }
                )
            for target_pct in [50.0, 90.0, 99.0]:
                target_eval = sub.loc[
                    (100.0 * sub["eval_90"].rank(method="max", pct=True)) >= target_pct,
                    "eval_90",
                ].min()
                threshold_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "target_share_pct": target_pct,
                        "evaluation_threshold": int(target_eval) if pd.notna(target_eval) else np.nan,
                    }
                )

        comparison_rows.append(parameter_comparison(family, family_early_df, family_late_df))

    percentile_df = pd.DataFrame(percentile_rows).sort_values("family")
    cumulative_df = pd.DataFrame(cumulative_rows).sort_values(["family", "front_type", "evaluation"])
    threshold_df = pd.DataFrame(threshold_rows).sort_values(["family", "front_type", "target_share_pct"])
    comparison_df = pd.concat(comparison_rows, ignore_index=True).sort_values(["family", "parameter"])

    eval_df.to_csv(TABLE_ROOT / "hv90_eval_all.csv", index=False)
    percentile_df.to_csv(TABLE_ROOT / "hv90_percentiles.csv", index=False)
    cumulative_df.to_csv(TABLE_ROOT / "hv90_cumulative_distribution.csv", index=False)
    threshold_df.to_csv(TABLE_ROOT / "hv90_thresholds.csv", index=False)
    comparison_df.to_csv(TABLE_ROOT / "hv90_parameter_comparison.csv", index=False)

    compact_df = percentile_df.rename(
        columns={
            "family": "Benchmark",
            "p10": "P10",
            "p25": "P25",
            "median": "Median",
            "p75": "P75",
            "p90": "P90",
        }
    )
    compact_df.to_csv(report_table_path("hv90_percentiles.csv"), index=False)
    comparison_df.to_csv(report_table_path("hv90_parameter_comparison.csv"), index=False)

    figure_path = plot_cdf(cumulative_df)
    print(f"Saved: {TABLE_ROOT / 'hv90_eval_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'hv90_percentiles.csv'}")
    print(f"Saved: {TABLE_ROOT / 'hv90_cumulative_distribution.csv'}")
    print(f"Saved: {TABLE_ROOT / 'hv90_parameter_comparison.csv'}")
    print(f"Saved: {figure_path}")


if __name__ == "__main__":
    main()
