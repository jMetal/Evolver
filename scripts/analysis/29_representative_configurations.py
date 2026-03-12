"""Derive representative configurations per problem family."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

from config import TABLES_DIR
from configuration_analysis import (
    REPRESENTATIVE_CATEGORICALS,
    build_representative_configuration,
    configuration_to_cli_lines,
    report_table_path,
)
from data_loader import get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "representative_configurations"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)


def load_family_pool(family: str) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for front_type in ["referenceFronts", "extremePointsFronts"]:
        for budget in [1000, 3000, 5000, 7000]:
            final_best = get_final_best_configs(load_all_runs_with_config(family, front_type, budget)).copy()
            final_best["family"] = family
            final_best["front_type"] = front_type
            final_best["budget"] = budget
            frames.append(final_best)
    return pd.concat(frames, ignore_index=True)


def main() -> None:
    summary_rows: list[dict[str, object]] = []
    detail_rows: list[dict[str, object]] = []

    for family in ["RE3D", "RWA3D"]:
        pooled_df = load_family_pool(family)
        summary, subset = build_representative_configuration(pooled_df, REPRESENTATIVE_CATEGORICALS)

        configuration = {
            key: value
            for key, value in summary.items()
            if not key.startswith("mode_")
            and key not in {"matching_rows", "coverage_pct", "hv_median", "ep_median"}
            and key not in {"family", "front_type", "budget"}
        }
        cli_lines = configuration_to_cli_lines(configuration)
        (TABLE_ROOT / f"representative_configuration_{family}.txt").write_text(
            "\n".join(cli_lines) + "\n",
            encoding="ascii",
        )

        summary_rows.append(
            {
                "family": family,
                "matching_rows": summary["matching_rows"],
                "coverage_pct": summary["coverage_pct"],
                "hv_median": summary["hv_median"],
                "ep_median": summary["ep_median"],
                **{column: summary.get(f"mode_{column}") for column in REPRESENTATIVE_CATEGORICALS},
            }
        )

        for parameter, value in configuration.items():
            detail_rows.append(
                {
                    "family": family,
                    "parameter": parameter,
                    "value": value,
                }
            )

    summary_df = pd.DataFrame(summary_rows).sort_values("family")
    detail_df = pd.DataFrame(detail_rows).sort_values(["family", "parameter"])
    summary_df.to_csv(TABLE_ROOT / "representative_configurations_summary.csv", index=False)
    detail_df.to_csv(TABLE_ROOT / "representative_configurations_detail.csv", index=False)

    compact_df = summary_df.rename(
        columns={
            "family": "Benchmark",
            "matching_rows": "MatchingRows",
            "coverage_pct": "CoveragePct",
            "hv_median": "HVMedian",
            "ep_median": "EPMedian",
        }
    )
    compact_df.to_csv(report_table_path("representative_configurations_summary.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'representative_configurations_summary.csv'}")
    print(f"Saved: {TABLE_ROOT / 'representative_configurations_detail.csv'}")
    print(f"Saved: {report_table_path('representative_configurations_summary.csv')}")


if __name__ == "__main__":
    main()
