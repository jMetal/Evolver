"""
Figure 6 — External validation summary (RQ4).

This script reads the compact representative-configuration validation study and
summarizes median HV across the seen/unseen/full splits for the RE and RWA
 suites. It also exports a compact wins/ties/losses table against the default
 NSGA-II baseline.

If the validation bundle is not available locally, the script generates a
placeholder figure and a placeholder LaTeX table so the manuscript still
compiles cleanly.

Usage:
  python experiments/rq4_validation/generate_validation_summary.py
"""

from __future__ import annotations

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from rq4_validation_utils import (
    DISPLAY_NAMES,
    GENERATED_DIR,
    OUTPUT_DIR,
    SPLIT_ORDER,
    STANDARD_TAG,
    SUITE_CONFIGS,
    aggregate_by_split,
    draw_placeholder_figure,
    load_validation_bundle,
    pairwise_against_standard,
    save_placeholder_tex,
    split_positions,
    wins_ties_losses,
)

matplotlib.rcParams.update(
    {
        "font.family": "serif",
        "font.size": 10,
        "axes.linewidth": 0.8,
        "xtick.major.width": 0.8,
        "ytick.major.width": 0.8,
        "axes.grid": True,
        "grid.linewidth": 0.35,
        "grid.alpha": 0.30,
        "grid.color": "#bbbbbb",
    }
)

FIGURE_BASENAME = OUTPUT_DIR / "validation_summary_rq4"
SUMMARY_CSV = GENERATED_DIR / "validation_summary_rq4.csv"
WTL_TEX = GENERATED_DIR / "validation_wtl_rq4.tex"
PAIRWISE_CSV = GENERATED_DIR / "validation_pairwise_rq4.csv"

STYLES = {
    STANDARD_TAG: dict(color="#111111", marker="o", linestyle="-", linewidth=2.2),
    "Complete-RE3D": dict(color="#2166ac", marker="s", linestyle="-", linewidth=1.7),
    "Extreme-RE3D": dict(color="#2166ac", marker="^", linestyle="--", linewidth=1.5),
    "Complete-RWA3D": dict(color="#b35806", marker="D", linestyle="-", linewidth=1.7),
    "Extreme-RWA3D": dict(color="#b35806", marker="v", linestyle="--", linewidth=1.5),
}


def write_wtl_table(aggregated: pd.DataFrame, wtl: pd.DataFrame) -> None:
    rows: list[str] = []
    for suite in ("RE", "RWA"):
        for split in SPLIT_ORDER:
            for tag in SUITE_CONFIGS[suite]["own"] + SUITE_CONFIGS[suite]["cross"]:
                hv = aggregated[
                    (aggregated["Suite"] == suite)
                    & (aggregated["Algorithm"] == tag)
                    & (aggregated["Split"] == split)
                    & (aggregated["IndicatorName"] == "HV")
                ]["SuiteMedian"]
                ep = aggregated[
                    (aggregated["Suite"] == suite)
                    & (aggregated["Algorithm"] == tag)
                    & (aggregated["Split"] == split)
                    & (aggregated["IndicatorName"] == "EP")
                ]["SuiteMedian"]
                wtl_value = wtl[
                    (wtl["Suite"] == suite)
                    & (wtl["Algorithm"] == tag)
                    & (wtl["Split"] == split)
                ]["wtl"]
                if hv.empty or ep.empty or wtl_value.empty:
                    continue
                rows.append(
                    f"{suite} & {split} & {DISPLAY_NAMES[tag]} & "
                    f"{hv.iloc[0]:.4f} & {ep.iloc[0]:.4f} & {wtl_value.iloc[0]} \\\\"
                )

    content = [
        "\\begin{table}[htbp]",
        "\\centering",
        "\\caption{Compact RQ4 validation summary across the seen, unseen, and full splits. HV reports the median of the per-problem median hypervolume values; W/T/L counts compare each tuned configuration against the default NSGA-II baseline on the same split.}",
        "\\small",
        "\\begin{tabular}{llllll}",
        "\\toprule",
        "Suite & Split & Config & Median HV & Median EP & W/T/L \\\\",
        "\\midrule",
    ]
    if rows:
        content.extend(rows)
    else:
        content.append("RE & seen & pending & -- & -- & -- \\\\")
    content.extend(
        [
            "\\bottomrule",
            "\\end{tabular}",
            "\\end{table}",
            "",
        ]
    )
    WTL_TEX.write_text("\n".join(content), encoding="utf-8")


def main() -> None:
    bundles = {suite: load_validation_bundle(suite) for suite in ("RE", "RWA")}
    if any(bundle is None for bundle in bundles.values()):
        draw_placeholder_figure(
            FIGURE_BASENAME,
            "RQ4 validation bundle not found",
            "Run RepresentativeConfigurationValidationStudy first.\n"
            "Expected outputs under experiments/rq4_validation/results/representative-configs/\n"
            "RepresentativeConfigsRE and RepresentativeConfigsRWA.",
        )
        save_placeholder_tex(
            WTL_TEX,
            "Compact RQ4 validation summary across the seen, unseen, and full splits.",
        )
        pd.DataFrame(
            columns=["Suite", "Algorithm", "Split", "IndicatorName", "SuiteMedian"]
        ).to_csv(SUMMARY_CSV, index=False)
        pd.DataFrame(
            columns=[
                "Suite",
                "Problem",
                "Split",
                "IndicatorName",
                "Algorithm",
                "Baseline",
                "CandidateMedian",
                "BaselineMedian",
                "PValue",
                "A12",
                "Outcome",
            ]
        ).to_csv(PAIRWISE_CSV, index=False)
        return

    summary = pd.concat([bundles["RE"]["summary"], bundles["RWA"]["summary"]], ignore_index=True)
    aggregated = aggregate_by_split(summary)
    aggregated.to_csv(SUMMARY_CSV, index=False)
    pairwise_against_standard(summary).to_csv(PAIRWISE_CSV, index=False)
    wtl = wins_ties_losses(summary, indicator_name="HV")
    write_wtl_table(aggregated, wtl)

    fig, axes = plt.subplots(1, 2, figsize=(11.0, 4.6), sharey=False)
    x = split_positions()

    for ax, suite in zip(axes, ("RE", "RWA")):
        suite_tags = [STANDARD_TAG] + SUITE_CONFIGS[suite]["own"] + SUITE_CONFIGS[suite]["cross"]
        for tag in suite_tags:
            line = aggregated[
                (aggregated["Suite"] == suite)
                & (aggregated["Algorithm"] == tag)
                & (aggregated["IndicatorName"] == "HV")
            ].set_index("Split")
            y = [line.loc[split, "SuiteMedian"] if split in line.index else np.nan for split in SPLIT_ORDER]
            ax.plot(x, y, label=DISPLAY_NAMES[tag], **STYLES[tag])

        ax.set_title(f"{suite} validation suite", fontsize=11)
        ax.set_xticks(x)
        ax.set_xticklabels([split.capitalize() for split in SPLIT_ORDER])
        ax.set_xlabel("Validation split", fontsize=10)
        ax.tick_params(which="both", direction="in")
        ax.spines["top"].set_visible(False)
        ax.spines["right"].set_visible(False)
        ax.grid(axis="x", visible=False)
        ax.grid(axis="y")

    axes[0].set_ylabel("Median HV across problem medians", fontsize=10)
    axes[1].legend(loc="best", fontsize=8.2, framealpha=0.92, edgecolor="#cccccc")
    fig.suptitle(
        "External validation and cross-suite transfer of the representative 7k configurations (RQ4)",
        fontsize=11,
        y=1.02,
    )
    plt.tight_layout()

    for ext in ("png", "pdf"):
        fig.savefig(FIGURE_BASENAME.with_suffix(f".{ext}"), dpi=300, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    main()
