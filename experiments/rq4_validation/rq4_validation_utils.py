from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

try:
    from scipy.stats import mannwhitneyu
except ImportError:  # pragma: no cover
    mannwhitneyu = None

REPO_ROOT = Path(__file__).resolve().parents[2]
EXPERIMENT_DIR = Path(__file__).resolve().parent
OUTPUT_DIR = EXPERIMENT_DIR / "generated"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
GENERATED_DIR = EXPERIMENT_DIR / "generated"
GENERATED_DIR.mkdir(parents=True, exist_ok=True)
VALIDATION_BASE_DIR = EXPERIMENT_DIR / "results" / "representative-configs"

SPLIT_ORDER = ["seen", "unseen", "full"]
STANDARD_TAG = "NSGAII-Standard"
SUITE_CONFIGS = {
    "RE": {
        "experiment": "RepresentativeConfigsRE",
        "own": ["Complete-RE3D", "Extreme-RE3D"],
        "cross": ["Complete-RWA3D", "Extreme-RWA3D"],
    },
    "RWA": {
        "experiment": "RepresentativeConfigsRWA",
        "own": ["Complete-RWA3D", "Extreme-RWA3D"],
        "cross": ["Complete-RE3D", "Extreme-RE3D"],
    },
}

DISPLAY_NAMES = {
    STANDARD_TAG: "Standard",
    "Complete-RE3D": "Complete-RE3D",
    "Extreme-RE3D": "Extreme-RE3D",
    "Complete-RWA3D": "Complete-RWA3D",
    "Extreme-RWA3D": "Extreme-RWA3D",
}


def experiment_dir(suite: str, ablation_base_tag: str | None = None) -> Path:
    directory = VALIDATION_BASE_DIR / SUITE_CONFIGS[suite]["experiment"]
    if ablation_base_tag is not None:
        directory = VALIDATION_BASE_DIR / (
            f"{SUITE_CONFIGS[suite]['experiment']}-Ablation-{ablation_base_tag.replace('-', '_')}"
        )
    return directory


def load_validation_bundle(suite: str, ablation_base_tag: str | None = None) -> dict | None:
    directory = experiment_dir(suite, ablation_base_tag)
    summary_path = directory / "QualityIndicatorSummary.csv"
    splits_path = directory / "metadata_problem_splits.csv"
    algorithms_path = directory / "metadata_algorithms.csv"

    if not summary_path.is_file() or not splits_path.is_file() or not algorithms_path.is_file():
        return None

    summary = pd.read_csv(summary_path)
    splits = pd.read_csv(splits_path)
    algorithms = pd.read_csv(algorithms_path)

    summary["IndicatorValue"] = pd.to_numeric(summary["IndicatorValue"])
    splits = splits.rename(columns={"problem": "Problem", "split": "Split"})
    summary = summary.merge(splits, on="Problem", how="left")

    full_rows = summary.copy()
    full_rows["Split"] = "full"
    summary = pd.concat([summary, full_rows], ignore_index=True)
    summary["Suite"] = suite

    return {
        "directory": directory,
        "summary": summary,
        "algorithms": algorithms,
    }


def aggregate_by_split(summary: pd.DataFrame) -> pd.DataFrame:
    per_problem = (
        summary.groupby(["Suite", "Algorithm", "Problem", "Split", "IndicatorName"], as_index=False)[
            "IndicatorValue"
        ]
        .median()
        .rename(columns={"IndicatorValue": "ProblemMedian"})
    )

    aggregated = (
        per_problem.groupby(["Suite", "Algorithm", "Split", "IndicatorName"], as_index=False)[
            "ProblemMedian"
        ]
        .median()
        .rename(columns={"ProblemMedian": "SuiteMedian"})
    )

    return aggregated


def wins_ties_losses(summary: pd.DataFrame, indicator_name: str = "HV") -> pd.DataFrame:
    per_problem = (
        summary[summary["IndicatorName"] == indicator_name]
        .groupby(["Suite", "Algorithm", "Problem", "Split"], as_index=False)["IndicatorValue"]
        .median()
        .rename(columns={"IndicatorValue": "ProblemMedian"})
    )
    baseline = (
        per_problem[per_problem["Algorithm"] == STANDARD_TAG]
        .rename(columns={"ProblemMedian": "BaselineMedian"})
        .drop(columns=["Algorithm"])
    )
    merged = per_problem.merge(baseline, on=["Suite", "Problem", "Split"], how="inner")
    merged = merged[merged["Algorithm"] != STANDARD_TAG].copy()

    merged["outcome"] = "tie"
    better = merged["ProblemMedian"] > merged["BaselineMedian"]
    worse = merged["ProblemMedian"] < merged["BaselineMedian"]
    merged.loc[better, "outcome"] = "win"
    merged.loc[worse, "outcome"] = "loss"

    counts = (
        merged.groupby(["Suite", "Algorithm", "Split", "outcome"])
        .size()
        .unstack(fill_value=0)
        .reset_index()
    )
    for column in ["win", "tie", "loss"]:
        if column not in counts.columns:
            counts[column] = 0

    counts["wtl"] = counts["win"].astype(str) + "/" + counts["tie"].astype(str) + "/" + counts["loss"].astype(str)
    return counts


def pairwise_against_standard(summary: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []

    for (suite, problem, split, indicator_name), frame in summary.groupby(
        ["Suite", "Problem", "Split", "IndicatorName"], sort=True
    ):
        baseline = frame[frame["Algorithm"] == STANDARD_TAG]["IndicatorValue"].to_numpy(dtype=float)
        if baseline.size == 0:
            continue

        higher_is_better = indicator_name == "HV"
        baseline_median = float(np.median(baseline))

        for algorithm, candidate_frame in frame.groupby("Algorithm", sort=True):
            if algorithm == STANDARD_TAG:
                continue

            candidate = candidate_frame["IndicatorValue"].to_numpy(dtype=float)
            if candidate.size == 0:
                continue

            candidate_median = float(np.median(candidate))
            p_value = np.nan
            if mannwhitneyu is not None:
                p_value = float(mannwhitneyu(candidate, baseline, alternative="two-sided").pvalue)

            if higher_is_better:
                better = candidate_median > baseline_median
                worse = candidate_median < baseline_median
            else:
                better = candidate_median < baseline_median
                worse = candidate_median > baseline_median

            outcome = "tie"
            if better:
                outcome = "win"
            elif worse:
                outcome = "loss"

            rows.append(
                {
                    "Suite": suite,
                    "Problem": problem,
                    "Split": split,
                    "IndicatorName": indicator_name,
                    "Algorithm": algorithm,
                    "Baseline": STANDARD_TAG,
                    "CandidateMedian": candidate_median,
                    "BaselineMedian": baseline_median,
                    "PValue": p_value,
                    "A12": vargha_delaney_a12(candidate, baseline, higher_is_better=higher_is_better),
                    "Outcome": outcome,
                }
            )

    return pd.DataFrame(rows)


def choose_winner(aggregated: pd.DataFrame, suite: str) -> str | None:
    own_tags = SUITE_CONFIGS[suite]["own"]
    candidates = aggregated[
        (aggregated["Suite"] == suite)
        & (aggregated["Algorithm"].isin(own_tags))
        & (aggregated["Split"] == "unseen")
    ]
    hv = candidates[candidates["IndicatorName"] == "HV"][["Algorithm", "SuiteMedian"]].rename(
        columns={"SuiteMedian": "HV"}
    )
    ep = candidates[candidates["IndicatorName"] == "EP"][["Algorithm", "SuiteMedian"]].rename(
        columns={"SuiteMedian": "EP"}
    )
    ranking = hv.merge(ep, on="Algorithm", how="outer").dropna()
    if ranking.empty:
        return None

    ranking = ranking.sort_values(["HV", "EP"], ascending=[False, True]).reset_index(drop=True)
    return str(ranking.loc[0, "Algorithm"])


def vargha_delaney_a12(
    sample_a: np.ndarray, sample_b: np.ndarray, *, higher_is_better: bool
) -> float:
    a = np.asarray(sample_a, dtype=float)
    b = np.asarray(sample_b, dtype=float)
    if a.size == 0 or b.size == 0:
        return float("nan")

    if higher_is_better:
        wins = np.sum(a[:, None] > b[None, :])
        ties = np.sum(a[:, None] == b[None, :])
    else:
        wins = np.sum(a[:, None] < b[None, :])
        ties = np.sum(a[:, None] == b[None, :])

    return float((wins + 0.5 * ties) / (a.size * b.size))


def save_placeholder_tex(path: Path, caption: str) -> None:
    path.write_text(
        "\\begin{table}[htbp]\n"
        "\\centering\n"
        "\\caption{" + caption + "}\n"
        "\\begin{tabular}{ll}\n"
        "\\toprule\n"
        "Artifact & Status \\\\\n"
        "\\midrule\n"
        "Validation bundle & pending local execution \\\\\n"
        "\\bottomrule\n"
        "\\end{tabular}\n"
        "\\end{table}\n",
        encoding="utf-8",
    )


def draw_placeholder_figure(output_path: Path, title: str, message: str) -> None:
    fig, ax = plt.subplots(figsize=(9.5, 3.8))
    ax.axis("off")
    ax.text(
        0.5,
        0.62,
        title,
        ha="center",
        va="center",
        fontsize=14,
        fontweight="bold",
        transform=ax.transAxes,
    )
    ax.text(
        0.5,
        0.38,
        message,
        ha="center",
        va="center",
        fontsize=10.5,
        transform=ax.transAxes,
        linespacing=1.4,
    )
    for ext in ("png", "pdf"):
        fig.savefig(output_path.with_suffix(f".{ext}"), dpi=300, bbox_inches="tight")
    plt.close(fig)


def split_positions() -> np.ndarray:
    return np.arange(len(SPLIT_ORDER), dtype=float)
