"""
Figure 6c — Critical difference diagram for the compact RQ4 validation set.

This script pools the RE and RWA validation suites, computes per-problem
median indicator values for the five candidate configurations, and generates
Demšar-style critical difference diagrams for HV and EP.

If the validation bundles are not available locally, the script emits a
placeholder figure so the manuscript still compiles cleanly.

Usage:
  python experiments/rq4_validation/generate_validation_cd_diagram.py
"""

from __future__ import annotations

import math

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

try:
    from scipy.stats import friedmanchisquare
except ImportError:  # pragma: no cover
    friedmanchisquare = None

from rq4_validation_utils import (
    GENERATED_DIR,
    OUTPUT_DIR,
    STANDARD_TAG,
    draw_placeholder_figure,
    load_validation_bundle,
)

matplotlib.rcParams.update(
    {
        "font.family": "serif",
        "font.size": 10,
        "axes.linewidth": 0.8,
    }
)

FIGURE_BASENAME = OUTPUT_DIR / "validation_cd_rq4"
RANKS_CSV = GENERATED_DIR / "validation_cd_rq4.csv"

DISPLAY_NAMES = {
    STANDARD_TAG: "NSGA-II\nStandard",
    "Complete-RE3D": "Complete-\nRE3D",
    "Extreme-RE3D": "Extreme-\nRE3D",
    "Complete-RWA3D": "Complete-\nRWA3D",
    "Extreme-RWA3D": "Extreme-\nRWA3D",
}

COLORS = {
    STANDARD_TAG: "#111111",
    "Complete-RE3D": "#2166ac",
    "Extreme-RE3D": "#4393c3",
    "Complete-RWA3D": "#b35806",
    "Extreme-RWA3D": "#f1a340",
}

NEMENYI_Q_ALPHA_005 = {
    2: 1.960,
    3: 2.343,
    4: 2.569,
    5: 2.728,
    6: 2.850,
    7: 2.949,
    8: 3.031,
    9: 3.102,
    10: 3.164,
}


def fractional_rank(values: np.ndarray, *, higher_is_better: bool) -> np.ndarray:
    order = np.argsort(-values if higher_is_better else values)
    ranks = np.zeros(values.size, dtype=float)
    sorted_vals = values[order]
    i = 0
    while i < values.size:
        j = i
        while j + 1 < values.size and abs(sorted_vals[j + 1] - sorted_vals[i]) < 1e-12:
            j += 1
        avg_rank = (i + 1 + j + 1) / 2.0
        for k in range(i, j + 1):
            ranks[order[k]] = avg_rank
        i = j + 1
    return ranks


def nemenyi_q_alpha(k: int) -> float:
    if k in NEMENYI_Q_ALPHA_005:
        return NEMENYI_Q_ALPHA_005[k]
    if k < 2:
        return NEMENYI_Q_ALPHA_005[2]
    if k > 10:
        return NEMENYI_Q_ALPHA_005[10]
    return NEMENYI_Q_ALPHA_005[4]


def build_rank_summary(summary: pd.DataFrame, indicator_name: str, *, higher_is_better: bool) -> dict[str, object]:
    per_problem = (
        summary[
            (summary["IndicatorName"] == indicator_name)
            & (summary["Split"] != "full")
        ]
        .groupby(["Suite", "Problem", "Algorithm"], as_index=False)["IndicatorValue"]
        .median()
    )

    matrix = per_problem.pivot_table(
        index=["Suite", "Problem"],
        columns="Algorithm",
        values="IndicatorValue",
    ).dropna()

    algorithms = list(matrix.columns)
    values = matrix.to_numpy(dtype=float)
    rank_matrix = np.vstack(
        [fractional_rank(row, higher_is_better=higher_is_better) for row in values]
    )
    mean_ranks = rank_matrix.mean(axis=0)

    if friedmanchisquare is None:
        friedman_stat = float("nan")
        friedman_p = float("nan")
    else:
        samples = [(-values[:, i] if higher_is_better else values[:, i]) for i in range(values.shape[1])]
        friedman_stat, friedman_p = friedmanchisquare(*samples)

    cd = nemenyi_q_alpha(len(algorithms)) * math.sqrt(len(algorithms) * (len(algorithms) + 1) / (6.0 * values.shape[0]))

    return {
        "algorithms": algorithms,
        "mean_ranks": {algorithm: float(mean_ranks[i]) for i, algorithm in enumerate(algorithms)},
        "friedman_stat": float(friedman_stat),
        "friedman_p": float(friedman_p),
        "cd": float(cd),
        "n_problems": int(values.shape[0]),
    }


def draw_cd_diagram(
    ax: plt.Axes,
    *,
    mean_ranks: dict[str, float],
    cd: float,
    title: str,
) -> None:
    sorted_algorithms = sorted(mean_ranks.items(), key=lambda item: item[1])
    n_algorithms = len(sorted_algorithms)

    min_rank = 1.0
    max_rank = float(n_algorithms)
    ax.set_xlim(min_rank - 0.2, max_rank + 0.2)
    ax.set_ylim(-1.35, 1.85)
    ax.axis("off")

    ax.plot([min_rank, max_rank], [0.0, 0.0], color="black", linewidth=1.1, zorder=3)
    for rank in range(1, n_algorithms + 1):
        ax.plot([rank, rank], [-0.07, 0.07], color="black", linewidth=0.8)
        ax.text(rank, -0.19, str(rank), ha="center", va="top", fontsize=9)

    left_y = 0.65
    right_y = -0.62
    left_step = 0.36
    right_step = 0.36
    left_count = 0
    right_count = 0

    for index, (algorithm, rank) in enumerate(sorted_algorithms):
        if index % 2 == 0:
            y = left_y + left_step * left_count
            text_y = y + 0.10
            va = "bottom"
            ha = "right"
            text_x = rank - 0.32
            left_count += 1
        else:
            y = right_y - right_step * right_count
            text_y = y - 0.10
            va = "top"
            ha = "left"
            text_x = rank + 0.32
            right_count += 1

        color = COLORS[algorithm]
        ax.plot([rank, rank], [0.0, y], color="#777777", linewidth=0.8, zorder=2)
        ax.scatter([rank], [0.0], marker="v", s=70, color=color, zorder=4)
        ax.text(
            text_x,
            text_y,
            f"{DISPLAY_NAMES[algorithm]} ({rank:.2f})",
            ha=ha,
            va=va,
            fontsize=8.5,
            color=color,
            fontweight="bold" if algorithm == STANDARD_TAG else "normal",
        )

    groups: list[tuple[float, float]] = []
    start = 0
    while start < n_algorithms - 1:
        end = start
        while end + 1 < n_algorithms and (sorted_algorithms[end + 1][1] - sorted_algorithms[start][1]) <= cd:
            end += 1
        if end > start:
            groups.append((sorted_algorithms[start][1], sorted_algorithms[end][1]))
        start = end + 1 if end > start else start + 1

    bar_y = -1.0
    for left_rank, right_rank in groups:
        ax.plot(
            [left_rank, right_rank],
            [bar_y, bar_y],
            color="#9e9e9e",
            linewidth=3.2,
            solid_capstyle="round",
            alpha=0.75,
            zorder=1,
        )
        bar_y -= 0.13

    cd_start = 1.0
    cd_end = cd_start + cd
    ax.plot([cd_start, cd_end], [1.45, 1.45], color="black", linewidth=1.4)
    ax.plot([cd_start, cd_start], [1.38, 1.52], color="black", linewidth=1.0)
    ax.plot([cd_end, cd_end], [1.38, 1.52], color="black", linewidth=1.0)
    ax.text((cd_start + cd_end) / 2.0, 1.57, f"CD = {cd:.2f}", ha="center", va="bottom", fontsize=9)
    ax.set_title(title, fontsize=10.5, pad=2)


def main() -> None:
    bundles = {suite: load_validation_bundle(suite) for suite in ("RE", "RWA")}
    if any(bundle is None for bundle in bundles.values()):
        draw_placeholder_figure(
            FIGURE_BASENAME,
            "RQ4 validation bundle not found",
            "Critical difference diagram pending local execution of the validation bundles.",
        )
        pd.DataFrame(
            columns=["IndicatorName", "Algorithm", "MeanRank", "CriticalDifference", "FriedmanStatistic", "FriedmanPValue", "Problems"]
        ).to_csv(RANKS_CSV, index=False)
        return

    summary = pd.concat([bundles["RE"]["summary"], bundles["RWA"]["summary"]], ignore_index=True)

    hv_summary = build_rank_summary(summary, "HV", higher_is_better=True)
    ep_summary = build_rank_summary(summary, "EP", higher_is_better=False)

    rows = []
    for indicator_name, indicator_summary in (("HV", hv_summary), ("EP", ep_summary)):
        for algorithm, mean_rank in indicator_summary["mean_ranks"].items():
            rows.append(
                {
                    "IndicatorName": indicator_name,
                    "Algorithm": algorithm,
                    "MeanRank": mean_rank,
                    "CriticalDifference": indicator_summary["cd"],
                    "FriedmanStatistic": indicator_summary["friedman_stat"],
                    "FriedmanPValue": indicator_summary["friedman_p"],
                    "Problems": indicator_summary["n_problems"],
                }
            )
    pd.DataFrame(rows).to_csv(RANKS_CSV, index=False)

    fig, axes = plt.subplots(1, 2, figsize=(11.2, 4.8))
    draw_cd_diagram(
        axes[0],
        mean_ranks=hv_summary["mean_ranks"],
        cd=hv_summary["cd"],
        title=(
            "Hypervolume (HV)\n"
            f"{hv_summary['n_problems']} pooled validation problems, "
            f"Friedman $p={hv_summary['friedman_p']:.2g}$"
        ),
    )
    draw_cd_diagram(
        axes[1],
        mean_ranks=ep_summary["mean_ranks"],
        cd=ep_summary["cd"],
        title=(
            "Epsilon (EP)\n"
            f"{ep_summary['n_problems']} pooled validation problems, "
            f"Friedman $p={ep_summary['friedman_p']:.2g}$"
        ),
    )
    fig.suptitle(
        "Validation ranking diagnostics across the pooled RE and RWA problem sets (RQ4)",
        fontsize=11,
        y=1.04,
    )
    plt.tight_layout()

    for ext in ("png", "pdf"):
        fig.savefig(FIGURE_BASENAME.with_suffix(f".{ext}"), dpi=300, bbox_inches="tight")
    plt.close(fig)


if __name__ == "__main__":
    main()
