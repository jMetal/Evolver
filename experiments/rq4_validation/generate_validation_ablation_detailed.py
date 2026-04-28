"""
Detailed forward-ablation figures for each baseline-to-target comparison.

For every representative configuration, this script generates a standalone
figure focused on the chosen path from NSGAII-Standard to the target on the
unseen split, reporting only HV.

Usage:
  python experiments/rq4_validation/generate_validation_ablation_detailed.py
"""

from __future__ import annotations

import re
import textwrap
from pathlib import Path

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from rq4_validation_utils import GENERATED_DIR, STANDARD_TAG, aggregate_by_split, load_validation_bundle

matplotlib.rcParams.update(
    {
        "font.family": "serif",
        "font.size": 9,
        "axes.linewidth": 0.8,
        "xtick.major.width": 0.8,
        "ytick.major.width": 0.8,
        "axes.grid": True,
        "grid.linewidth": 0.35,
        "grid.alpha": 0.30,
        "grid.color": "#bbbbbb",
    }
)

ABLATION_CONFIGS = [
    ("RE", "Complete-RE3D"),
    ("RE", "Extreme-RE3D"),
    ("RWA", "Complete-RWA3D"),
    ("RWA", "Extreme-RWA3D"),
]

ACCENT_BY_SUITE = {
    "RE": "#2166ac",
    "RWA": "#b35806",
}

DETAIL_DIR = GENERATED_DIR / "validation_ablation_detailed"
DETAIL_DIR.mkdir(parents=True, exist_ok=True)
CANDIDATE_CSV = GENERATED_DIR / "validation_ablation_detailed_candidates.csv"
PATH_CSV = GENERATED_DIR / "validation_ablation_detailed_path.csv"


def slugify(tag: str) -> str:
    return tag.lower().replace("-", "_")


def wrap_label(text: str, width: int = 28) -> str:
    return textwrap.fill(text, width=width, break_long_words=False, break_on_hyphens=False)


def candidate_label(tag: str, target_tag: str) -> str:
    if tag == STANDARD_TAG:
        return "Default"
    if tag == target_tag:
        return "Target"
    match = re.search(r"-Forward-S\d\d-(.*)$", tag)
    if match:
        return match.group(1)
    return tag


def short_change_label(text: str) -> str:
    if text in {"Default", "Target"}:
        return text
    return text.replace(";", " + ")


def aggregate_step_summary(summary_path: Path) -> pd.DataFrame:
    summary = pd.read_csv(summary_path)
    summary["IndicatorValue"] = pd.to_numeric(summary["IndicatorValue"])

    per_problem = (
        summary.groupby(["Algorithm", "Problem", "IndicatorName"], as_index=False)["IndicatorValue"]
        .median()
        .rename(columns={"IndicatorValue": "ProblemMedian"})
    )
    aggregated = (
        per_problem.groupby(["Algorithm", "IndicatorName"], as_index=False)["ProblemMedian"]
        .median()
        .rename(columns={"ProblemMedian": "SeenMedian"})
    )
    pivot = (
        aggregated.pivot(index="Algorithm", columns="IndicatorName", values="SeenMedian")
        .reset_index()
        .rename_axis(columns=None)
    )
    if "HV" not in pivot.columns:
        pivot["HV"] = np.nan
    if "EP" not in pivot.columns:
        pivot["EP"] = np.nan
    return pivot


def load_step_candidates(bundle: dict, suite: str, target_tag: str) -> pd.DataFrame:
    trajectory = bundle["trajectory"].copy()
    selected_by_step = {
        int(row.step): row.algorithm
        for row in trajectory.itertuples()
        if int(row.step) > 0 and bool(row.evaluated)
    }

    rows: list[dict[str, object]] = []
    steps_dir = bundle["directory"] / "_forward_steps"
    for step_dir in sorted(steps_dir.iterdir()):
        if not step_dir.is_dir() or not step_dir.name.startswith("step-") or step_dir.name == "step-00-source-target":
            continue
        step = int(step_dir.name.split("-")[1])
        summary_path = step_dir / "QualityIndicatorSummary.csv"
        if not summary_path.is_file():
            continue
        aggregated = aggregate_step_summary(summary_path)
        aggregated = aggregated.sort_values(["HV", "EP", "Algorithm"], ascending=[False, True, True])
        for rank, row in enumerate(aggregated.itertuples(index=False), start=1):
            label = candidate_label(str(row.Algorithm), target_tag)
            rows.append(
                {
                    "Suite": suite,
                    "Config": target_tag,
                    "Step": step,
                    "Rank": rank,
                    "Algorithm": str(row.Algorithm),
                    "Label": label,
                    "SeenHV": float(row.HV),
                    "SeenEP": float(row.EP),
                    "Selected": str(row.Algorithm) == selected_by_step.get(step),
                }
            )
    return pd.DataFrame(rows)


def load_selected_path(bundle: dict, suite: str, target_tag: str) -> pd.DataFrame:
    trajectory = bundle["trajectory"].copy()
    aggregated = aggregate_by_split(bundle["summary"])
    unseen = aggregated[aggregated["Split"] == "unseen"].copy()
    unseen = (
        unseen.pivot(index="Algorithm", columns="IndicatorName", values="SuiteMedian")
        .reset_index()
        .rename_axis(columns=None)
    )
    if "HV" not in unseen.columns:
        unseen["HV"] = np.nan
    if "EP" not in unseen.columns:
        unseen["EP"] = np.nan

    path = trajectory.merge(unseen, left_on="algorithm", right_on="Algorithm", how="left")
    path["Suite"] = suite
    path["Config"] = target_tag
    path["DisplayLabel"] = path["label"].map(short_change_label)
    return path


def draw_candidate_panel(ax: plt.Axes, frame: pd.DataFrame, accent: str) -> None:
    if frame.empty:
        ax.set_title("Selection on seen split (no data)", fontsize=10)
        return

    rows: list[dict[str, object]] = []
    y = 0
    step_centers: dict[int, float] = {}
    separators: list[float] = []
    ordered = frame.sort_values(["Step", "Rank"], ascending=[True, True])
    for step, step_frame in ordered.groupby("Step", sort=True):
        step_start = y
        for row in step_frame.itertuples(index=False):
            rows.append(
                {
                    "Y": y,
                    "HV": float(row.SeenHV),
                    "Label": wrap_label(str(row.Label), width=24),
                    "Selected": bool(row.Selected),
                    "Rank": int(row.Rank),
                }
            )
            y += 1
        step_end = y - 1
        step_centers[int(step)] = (step_start + step_end) / 2.0
        separators.append(y - 0.5)
        y += 1

    min_hv = min(row["HV"] for row in rows)
    max_hv = max(row["HV"] for row in rows)
    margin = max(0.002, 0.12 * (max_hv - min_hv if max_hv > min_hv else 0.01))

    for row in rows:
        color = accent if row["Selected"] else "#d9d9d9"
        edge = "#333333" if row["Selected"] else "#f5f5f5"
        ax.barh(
            row["Y"],
            row["HV"],
            color=color,
            alpha=0.92 if row["Selected"] else 0.88,
            edgecolor=edge,
            linewidth=0.8 if row["Selected"] else 0.6,
            height=0.8,
        )
        ax.text(
            row["HV"] + margin * 0.10,
            row["Y"],
            f"#{row['Rank']}  {row['Label']}",
            va="center",
            ha="left",
            fontsize=7.4,
            color="#222222",
        )

    for step, center in step_centers.items():
        ax.text(
            min_hv - margin * 0.9,
            center,
            f"S{step}",
            va="center",
            ha="right",
            fontsize=8.2,
            fontweight="bold",
            color="#444444",
        )
    for separator in separators[:-1]:
        ax.axhline(separator, color="#e0e0e0", linewidth=0.8)

    ax.set_title("Selection on seen split at each forward step", fontsize=10)
    ax.set_xlabel("Median HV on seen split", fontsize=9)
    ax.set_xlim(min_hv - margin, max_hv + margin * 5.5)
    ax.set_yticks([])
    ax.tick_params(which="both", direction="in")
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(axis="y", visible=False)
    ax.invert_yaxis()


def draw_path_panel(ax: plt.Axes, path: pd.DataFrame, metric: str, accent: str, title: str) -> None:
    values = path[metric].astype(float).to_numpy()
    steps = path["step"].astype(int).to_numpy()
    labels = [wrap_label(str(label), width=18) for label in path["DisplayLabel"]]
    ax.plot(steps, values, color=accent, marker="o", linewidth=1.8, markersize=5.0)
    for step, value in zip(steps, values):
        if np.isnan(value):
            continue
        ax.text(step, value, f" {value:.4f}", va="bottom", ha="left", fontsize=7.2, color="#333333")
    ax.set_title(title, fontsize=10)
    ax.set_xticks(steps)
    ax.set_xticklabels(labels, rotation=25, ha="right", fontsize=7.6)
    ax.tick_params(which="both", direction="in")
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.grid(axis="x", visible=False)


def draw_detailed_figure(
    suite: str,
    target_tag: str,
    path: pd.DataFrame,
) -> None:
    accent = ACCENT_BY_SUITE[suite]
    figure, ax_hv = plt.subplots(1, 1, figsize=(11.8, 4.8))

    draw_path_panel(ax_hv, path, "HV", accent, "Chosen path on unseen split (HV)")
    ax_hv.set_ylabel("Median HV", fontsize=9)

    figure.suptitle(
        f"Forward full ablation detail: {STANDARD_TAG} -> {target_tag} ({suite})",
        fontsize=13.5,
        y=0.98,
    )
    plt.tight_layout(rect=(0.03, 0.08, 0.98, 0.94))

    basename = DETAIL_DIR / f"validation_ablation_detailed_{slugify(target_tag)}"
    for ext in ("png", "pdf"):
        figure.savefig(basename.with_suffix(f".{ext}"), dpi=300, bbox_inches="tight")
        print(f"Saved: {basename.with_suffix(f'.{ext}')}")
    plt.close(figure)


def main() -> None:
    candidate_frames: list[pd.DataFrame] = []
    path_frames: list[pd.DataFrame] = []

    for suite, target_tag in ABLATION_CONFIGS:
        bundle = load_validation_bundle(suite, ablation_base_tag=target_tag, ablation_mode="forward")
        if bundle is None or bundle.get("trajectory") is None:
            print(f"Missing forward bundle for {target_tag} on {suite}")
            continue

        candidate_frame = load_step_candidates(bundle, suite, target_tag)
        path = load_selected_path(bundle, suite, target_tag)
        candidate_frames.append(candidate_frame)
        path_frames.append(path)
        draw_detailed_figure(suite, target_tag, path)

    if candidate_frames:
        pd.concat(candidate_frames, ignore_index=True).to_csv(CANDIDATE_CSV, index=False)
    else:
        pd.DataFrame(
            columns=["Suite", "Config", "Step", "Rank", "Algorithm", "Label", "SeenHV", "SeenEP", "Selected"]
        ).to_csv(CANDIDATE_CSV, index=False)

    if path_frames:
        pd.concat(path_frames, ignore_index=True).to_csv(PATH_CSV, index=False)
    else:
        pd.DataFrame().to_csv(PATH_CSV, index=False)


if __name__ == "__main__":
    main()
