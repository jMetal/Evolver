"""Representative configuration comparison across computational cost scenarios."""

from __future__ import annotations

import numpy as np
import pandas as pd

from config import TABLES_DIR
from configuration_analysis import (
    REPRESENTATIVE_CATEGORICALS,
    REPRESENTATIVE_NUMERICALS,
    build_representative_configuration,
    report_table_path,
)
from data_loader import best_config_per_evaluation, get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "cost_scenarios"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)

SCENARIOS = [
    {"scenario": "A", "budget": 1000, "meta_eval_cap": 1000, "label": "Budget 1000 / eval 1000"},
    {"scenario": "B", "budget": 3000, "meta_eval_cap": 1000, "label": "Budget 3000 / eval 1000"},
    {"scenario": "C", "budget": 7000, "meta_eval_cap": 3000, "label": "Budget 7000 / final"},
]


def scenario_rows(family: str, budget: int, meta_eval_cap: int) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for front_type in ["referenceFronts", "extremePointsFronts"]:
        traced_df = load_all_runs_with_config(family, front_type, budget).copy()
        if meta_eval_cap >= int(traced_df["Evaluation"].max()):
            selected = get_final_best_configs(traced_df)
        else:
            best_eval = best_config_per_evaluation(traced_df).copy()
            selected = best_eval.loc[best_eval["Evaluation"] == meta_eval_cap].copy()
        selected["family"] = family
        selected["front_type"] = front_type
        selected["budget"] = budget
        selected["meta_eval_cap"] = meta_eval_cap
        frames.append(selected)
    return pd.concat(frames, ignore_index=True)


def main() -> None:
    summary_rows: list[dict[str, object]] = []
    categorical_rows: list[dict[str, object]] = []
    numerical_rows: list[dict[str, object]] = []

    for family in ["RE3D", "RWA3D"]:
        scenario_summaries: dict[str, dict[str, object]] = {}
        family_summary_rows: dict[str, dict[str, object]] = {}
        for scenario in SCENARIOS:
            pooled_df = scenario_rows(family, scenario["budget"], scenario["meta_eval_cap"])
            summary, subset = build_representative_configuration(pooled_df, REPRESENTATIVE_CATEGORICALS)
            scenario_summaries[scenario["scenario"]] = summary
            family_summary_rows[scenario["scenario"]] = {
                "family": family,
                "scenario": scenario["scenario"],
                "label": scenario["label"],
                "budget": scenario["budget"],
                "meta_eval_cap": scenario["meta_eval_cap"],
                "matching_rows": summary["matching_rows"],
                "coverage_pct": summary["coverage_pct"],
                "hv_median": summary["hv_median"],
                "ep_median": summary["ep_median"],
                **{column: summary.get(f"mode_{column}") for column in REPRESENTATIVE_CATEGORICALS},
            }

        reference = scenario_summaries["C"]
        for scenario in ["A", "B", "C"]:
            current = scenario_summaries[scenario]
            match_count = 0
            for parameter in REPRESENTATIVE_CATEGORICALS:
                current_value = current.get(f"mode_{parameter}")
                reference_value = reference.get(f"mode_{parameter}")
                matches = bool(str(current_value) == str(reference_value))
                match_count += int(matches)
                categorical_rows.append(
                    {
                        "family": family,
                        "scenario": scenario,
                        "parameter": parameter,
                        "value": current_value,
                        "reference_value": reference_value,
                        "matches_reference_C": matches,
                    }
                )

            family_summary_rows[scenario]["categorical_match_count_vs_C"] = match_count

            for parameter in REPRESENTATIVE_NUMERICALS:
                current_value = current.get(parameter, np.nan)
                reference_value = reference.get(parameter, np.nan)
                pct_diff = (
                    100.0 * (float(current_value) - float(reference_value)) / abs(float(reference_value))
                    if pd.notna(current_value)
                    and pd.notna(reference_value)
                    and float(reference_value) != 0.0
                    else np.nan
                )
                numerical_rows.append(
                    {
                        "family": family,
                        "scenario": scenario,
                        "parameter": parameter,
                        "value": current_value,
                        "reference_value": reference_value,
                        "pct_diff_vs_C": pct_diff,
                    }
                )

        summary_rows.extend(family_summary_rows[scenario] for scenario in ["A", "B", "C"])

    summary_df = pd.DataFrame(summary_rows).sort_values(["family", "scenario"])
    categorical_df = pd.DataFrame(categorical_rows).sort_values(["family", "scenario", "parameter"])
    numerical_df = pd.DataFrame(numerical_rows).sort_values(["family", "scenario", "parameter"])

    summary_df.to_csv(TABLE_ROOT / "cost_scenario_summary.csv", index=False)
    categorical_df.to_csv(TABLE_ROOT / "cost_scenario_categorical_comparison.csv", index=False)
    numerical_df.to_csv(TABLE_ROOT / "cost_scenario_numerical_comparison.csv", index=False)

    compact_df = summary_df.rename(
        columns={
            "family": "Benchmark",
            "scenario": "Scenario",
            "label": "Label",
            "coverage_pct": "CoveragePct",
            "hv_median": "HVMedian",
            "ep_median": "EPMedian",
            "categorical_match_count_vs_C": "CategoricalMatchCountVsC",
        }
    )
    compact_df.to_csv(report_table_path("cost_scenario_summary.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'cost_scenario_summary.csv'}")
    print(f"Saved: {TABLE_ROOT / 'cost_scenario_categorical_comparison.csv'}")
    print(f"Saved: {TABLE_ROOT / 'cost_scenario_numerical_comparison.csv'}")
    print(f"Saved: {report_table_path('cost_scenario_summary.csv')}")


if __name__ == "__main__":
    main()
