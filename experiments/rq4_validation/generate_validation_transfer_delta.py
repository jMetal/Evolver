"""
Figure 6a — Unseen-split delta-HV view for RQ4 transfer asymmetry.

This script reads the split-level RQ4 summary and plots, for each target suite,
the change in median HV with respect to the default NSGA-II baseline on the
unseen split. The goal is to make the transfer asymmetry explicit.

Usage:
  python experiments/rq4_validation/generate_validation_transfer_delta.py
"""

from __future__ import annotations

from pathlib import Path

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.patches import Patch

from rq4_validation_utils import DISPLAY_NAMES, GENERATED_DIR, OUTPUT_DIR, STANDARD_TAG, draw_placeholder_figure

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

INPUT_CSV = GENERATED_DIR / "validation_summary_rq4.csv"
FIGURE_BASENAME = OUTPUT_DIR / "validation_transfer_delta_rq4"
OUTPUT_CSV = GENERATED_DIR / "validation_transfer_delta_rq4.csv"

ALGORITHM_ORDER = ["Complete-RE3D", "Extreme-RE3D", "Complete-RWA3D", "Extreme-RWA3D"]
SOURCE_COLORS = {"RE": "#2166ac", "RWA": "#b35806"}
FRONT_HATCHES = {"Complete": "", "Extreme": "//"}


def source_suite(algorithm: str) -> str:
    return "RE" if algorithm.endswith("RE3D") else "RWA"


def front_type(algorithm: str) -> str:
    return "Complete" if algorithm.startswith("Complete") else "Extreme"


def build_delta_frame(summary: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []

    for target_suite in ("RE", "RWA"):
        baseline = summary[
            (summary["Suite"] == target_suite)
            & (summary["Algorithm"] == STANDARD_TAG)
            & (summary["Split"] == "unseen")
            & (summary["IndicatorName"] == "HV")
        ]["SuiteMedian"]
        if baseline.empty:
            continue
        baseline_hv = float(baseline.iloc[0])

        for algorithm in ALGORITHM_ORDER:
            candidate = summary[
                (summary["Suite"] == target_suite)
                & (summary["Algorithm"] == algorithm)
                & (summary["Split"] == "unseen")
                & (summary["IndicatorName"] == "HV")
            ]["SuiteMedian"]
            if candidate.empty:
                continue

            candidate_hv = float(candidate.iloc[0])
            rows.append(
                {
                    "TargetSuite": target_suite,
                    "Algorithm": algorithm,
                    "BaselineHV": baseline_hv,
                    "CandidateHV": candidate_hv,
                    "DeltaHV": candidate_hv - baseline_hv,
                    "SourceSuite": source_suite(algorithm),
                    "FrontType": front_type(algorithm),
                }
            )

    return pd.DataFrame(rows)


def draw_delta_figure(frame: pd.DataFrame) -> None:
    fig, axes = plt.subplots(1, 2, figsize=(10.8, 4.5), sharey=True)

    for ax, target_suite in zip(axes, ("RE", "RWA")):
        suite_frame = frame[frame["TargetSuite"] == target_suite].copy()
        suite_frame["Algorithm"] = pd.Categorical(
            suite_frame["Algorithm"], categories=ALGORITHM_ORDER, ordered=True
        )
        suite_frame = suite_frame.sort_values("Algorithm")

        x = np.arange(len(suite_frame), dtype=float)
        bars = ax.bar(
            x,
            suite_frame["DeltaHV"],
            color=[SOURCE_COLORS[src] for src in suite_frame["SourceSuite"]],
            edgecolor="#333333",
            linewidth=0.8,
            alpha=0.90,
        )
        for bar, hatch in zip(bars, [FRONT_HATCHES[front] for front in suite_frame["FrontType"]]):
            bar.set_hatch(hatch)

        ax.axhline(0.0, color="#444444", linewidth=0.9)
        ax.set_xticks(x)
        ax.set_xticklabels([DISPLAY_NAMES[tag] for tag in suite_frame["Algorithm"]], rotation=25, ha="right")
        ax.set_title(f"Target suite: {target_suite}", fontsize=11)
        ax.set_xlabel("Representative configuration", fontsize=10)
        ax.tick_params(which="both", direction="in")
        ax.spines["top"].set_visible(False)
        ax.spines["right"].set_visible(False)
        ax.grid(axis="x", visible=False)

    axes[0].set_ylabel(r"$\Delta$HV vs. default on unseen split", fontsize=10)

    legend_handles = [
        Patch(facecolor=SOURCE_COLORS["RE"], edgecolor="#333333", label="RE-derived"),
        Patch(facecolor=SOURCE_COLORS["RWA"], edgecolor="#333333", label="RWA-derived"),
        Patch(facecolor="white", edgecolor="#333333", hatch=FRONT_HATCHES["Complete"], label="Complete"),
        Patch(facecolor="white", edgecolor="#333333", hatch=FRONT_HATCHES["Extreme"], label="Extreme"),
    ]
    axes[1].legend(handles=legend_handles, loc="best", fontsize=8.4, framealpha=0.92, edgecolor="#cccccc")

    fig.suptitle(
        "Unseen-split HV deltas highlight the transfer asymmetry of the representative 7k configurations",
        fontsize=11,
        y=1.02,
    )
    plt.tight_layout()

    for ext in ("png", "pdf"):
        fig.savefig(FIGURE_BASENAME.with_suffix(f".{ext}"), dpi=300, bbox_inches="tight")
    plt.close(fig)


def main() -> None:
    if not INPUT_CSV.is_file():
        draw_placeholder_figure(
            FIGURE_BASENAME,
            "RQ4 transfer-delta bundle not found",
            "Run generate_validation_summary.py first so validation_summary_rq4.csv is available.",
        )
        pd.DataFrame(
            columns=[
                "TargetSuite",
                "Algorithm",
                "BaselineHV",
                "CandidateHV",
                "DeltaHV",
                "SourceSuite",
                "FrontType",
            ]
        ).to_csv(OUTPUT_CSV, index=False)
        return

    summary = pd.read_csv(INPUT_CSV)
    frame = build_delta_frame(summary)
    frame.to_csv(OUTPUT_CSV, index=False)

    if frame.empty:
        draw_placeholder_figure(
            FIGURE_BASENAME,
            "RQ4 transfer-delta bundle not found",
            "validation_summary_rq4.csv exists, but it does not contain the unseen-split HV rows needed here.",
        )
        return

    draw_delta_figure(frame)


if __name__ == "__main__":
    main()
