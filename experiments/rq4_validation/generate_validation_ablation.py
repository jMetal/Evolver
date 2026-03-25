"""
Figure 6b — Compact real ablation summary for the RQ4 winners.

For each suite, the script chooses the better of the two in-suite representative
configurations using unseen-split HV as the primary criterion and unseen-split
EP as the tie-breaker. It then reads the corresponding ablation study and
plots the relative HV change caused by resetting archive, crossover, or mutation
to the standard NSGA-II defaults.

If either the validation bundle or the ablation bundle is not available, the
script generates a placeholder figure and CSV export.

Usage:
  python experiments/rq4_validation/generate_validation_ablation.py
"""

from __future__ import annotations

import matplotlib
import matplotlib.pyplot as plt
import pandas as pd

from rq4_validation_utils import (
    GENERATED_DIR,
    OUTPUT_DIR,
    aggregate_by_split,
    choose_winner,
    draw_placeholder_figure,
    load_validation_bundle,
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

FIGURE_BASENAME = OUTPUT_DIR / "validation_ablation_rq4"
SUMMARY_CSV = GENERATED_DIR / "validation_ablation_rq4.csv"


def main() -> None:
    validation_bundles = {suite: load_validation_bundle(suite) for suite in ("RE", "RWA")}
    if any(bundle is None for bundle in validation_bundles.values()):
        draw_placeholder_figure(
            FIGURE_BASENAME,
            "RQ4 ablation bundle not found",
            "Run the validation study first, then rerun it in ablation mode for the winner of each suite.",
        )
        pd.DataFrame(
            columns=["Suite", "Winner", "Variant", "HV", "RelativeHVChangePercent"]
        ).to_csv(SUMMARY_CSV, index=False)
        return

    validation_summary = pd.concat(
        [validation_bundles["RE"]["summary"], validation_bundles["RWA"]["summary"]],
        ignore_index=True,
    )
    validation_aggregated = aggregate_by_split(validation_summary)

    rows: list[dict[str, object]] = []
    for suite in ("RE", "RWA"):
        winner = choose_winner(validation_aggregated, suite)
        if winner is None:
            continue
        ablation_bundle = load_validation_bundle(suite, ablation_base_tag=winner)
        if ablation_bundle is None:
            continue

        ablation_aggregated = aggregate_by_split(ablation_bundle["summary"])
        unseen_hv = ablation_aggregated[
            (ablation_aggregated["Split"] == "unseen")
            & (ablation_aggregated["IndicatorName"] == "HV")
        ][["Algorithm", "SuiteMedian"]]
        base_value = unseen_hv[unseen_hv["Algorithm"] == winner]["SuiteMedian"]
        if base_value.empty:
            continue
        base_hv = float(base_value.iloc[0])

        for variant in [winner, f"{winner}-ArchiveReset", f"{winner}-CrossoverReset", f"{winner}-MutationReset"]:
            hv = unseen_hv[unseen_hv["Algorithm"] == variant]["SuiteMedian"]
            if hv.empty:
                continue
            value = float(hv.iloc[0])
            relative_change = 100.0 * (value - base_hv) / base_hv if base_hv != 0 else 0.0
            rows.append(
                {
                    "Suite": suite,
                    "Winner": winner,
                    "Variant": variant,
                    "HV": value,
                    "RelativeHVChangePercent": relative_change,
                }
            )

    frame = pd.DataFrame(rows)
    frame.to_csv(SUMMARY_CSV, index=False)
    if frame.empty:
        draw_placeholder_figure(
            FIGURE_BASENAME,
            "RQ4 ablation bundle not found",
            "Validation results exist, but the winner-specific ablation studies are still missing.",
        )
        return

    fig, axes = plt.subplots(1, 2, figsize=(10.5, 4.2), sharey=True)
    colors = {
        "ArchiveReset": "#1b9e77",
        "CrossoverReset": "#d95f02",
        "MutationReset": "#7570b3",
    }

    for ax, suite in zip(axes, ("RE", "RWA")):
        suite_frame = frame[frame["Suite"] == suite].copy()
        winner = suite_frame["Winner"].iloc[0]
        suite_frame = suite_frame[suite_frame["Variant"] != winner].copy()
        suite_frame["Label"] = suite_frame["Variant"].str.replace(f"{winner}-", "", regex=False)
        ax.bar(
            suite_frame["Label"],
            suite_frame["RelativeHVChangePercent"],
            color=[colors.get(label, "#999999") for label in suite_frame["Label"]],
            alpha=0.88,
        )
        ax.axhline(0.0, color="#444444", linewidth=0.9)
        ax.set_title(f"{suite} winner: {winner}", fontsize=10.5)
        ax.set_xlabel("Reset block", fontsize=10)
        ax.tick_params(which="both", direction="in")
        ax.spines["top"].set_visible(False)
        ax.spines["right"].set_visible(False)
        ax.grid(axis="x", visible=False)

    axes[0].set_ylabel("Relative HV change on unseen split (%)", fontsize=10)
    fig.suptitle(
        "Compact real ablation of the validation winners (RQ4 complement)",
        fontsize=11,
        y=1.02,
    )
    plt.tight_layout()

    for ext in ("png", "pdf"):
        fig.savefig(FIGURE_BASENAME.with_suffix(f".{ext}"), dpi=300, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    main()
