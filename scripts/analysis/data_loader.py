"""Utilities to load and summarize convergence traces from Evolver results."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

from config import RESULTS_ROOT

REQUIRED_COLUMNS = {"Evaluation", "SolutionId", "EP", "HVMinus"}


def experiment_path(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> Path:
    """Return the directory for one family/front/budget combination."""
    root = Path(results_root) if results_root is not None else RESULTS_ROOT
    return root / f"{family}.{front_type}.{budget}"


def available_runs(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> list[int]:
    """Discover available run folders for one experiment."""
    run_dirs = sorted(
        path
        for path in experiment_path(family, front_type, budget, results_root).glob("run*")
        if path.is_dir()
    )
    if not run_dirs:
        raise FileNotFoundError(
            f"No run folders found for {family}.{front_type}.{budget}"
        )
    run_ids: list[int] = []
    for run_dir in run_dirs:
        suffix = run_dir.name.removeprefix("run")
        if suffix.isdigit():
            run_ids.append(int(suffix))
    if not run_ids:
        raise FileNotFoundError(
            f"Run folders exist but do not match run<number> for {family}.{front_type}.{budget}"
        )
    return sorted(run_ids)


def load_indicators(
    family: str,
    front_type: str,
    budget: int,
    run: int,
    results_root: Path | None = None,
) -> pd.DataFrame:
    """Load one INDICATORS.csv file and validate its schema."""
    path = experiment_path(family, front_type, budget, results_root) / f"run{run}" / "INDICATORS.csv"
    df = pd.read_csv(path)
    missing_columns = REQUIRED_COLUMNS.difference(df.columns)
    if missing_columns:
        raise ValueError(f"Missing columns in {path}: {sorted(missing_columns)}")
    return df.drop_duplicates(subset=["Evaluation", "SolutionId"], keep="first")


def load_all_runs(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Load and concatenate all runs for one experiment."""
    frames: list[pd.DataFrame] = []
    for run in available_runs(family, front_type, budget, results_root):
        indicators = load_indicators(family, front_type, budget, run, results_root).copy()
        indicators["run"] = run
        frames.append(indicators)
    return pd.concat(frames, ignore_index=True)


def best_per_evaluation(indicators: pd.DataFrame) -> pd.DataFrame:
    """Keep the best EP and HVMinus found at each evaluation and run."""
    return (
        indicators.groupby(["run", "Evaluation"], as_index=False)
        .agg(best_EP=("EP", "min"), best_HVMinus=("HVMinus", "min"))
        .sort_values(["run", "Evaluation"])
    )


def convergence_summary(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Compute median and IQR envelopes for one experiment."""
    best = best_per_evaluation(load_all_runs(family, front_type, budget, results_root))
    return (
        best.groupby("Evaluation", as_index=False)
        .agg(
            ep_median=("best_EP", "median"),
            ep_q25=("best_EP", lambda values: values.quantile(0.25)),
            ep_q75=("best_EP", lambda values: values.quantile(0.75)),
            hv_median=("best_HVMinus", "median"),
            hv_q25=("best_HVMinus", lambda values: values.quantile(0.25)),
            hv_q75=("best_HVMinus", lambda values: values.quantile(0.75)),
        )
        .sort_values("Evaluation")
    )


def final_best_per_run(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Return the final best EP and HVMinus per run for one experiment."""
    best = best_per_evaluation(load_all_runs(family, front_type, budget, results_root))
    last_eval = best.groupby("run", as_index=False)["Evaluation"].max()
    return (
        best.merge(last_eval, on=["run", "Evaluation"], how="inner")
        .rename(columns={"best_EP": "final_best_EP", "best_HVMinus": "final_best_HVMinus"})
        .sort_values("run")
        .reset_index(drop=True)
    )


def final_best_summary(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> dict[str, float]:
    """Summarize final best values across runs for one experiment."""
    final_df = final_best_per_run(family, front_type, budget, results_root)
    return {
        "family": family,
        "front_type": front_type,
        "budget": budget,
        "runs": int(final_df["run"].nunique()),
        "final_best_EP_median": float(final_df["final_best_EP"].median()),
        "final_best_EP_q25": float(final_df["final_best_EP"].quantile(0.25)),
        "final_best_EP_q75": float(final_df["final_best_EP"].quantile(0.75)),
        "final_best_HVMinus_median": float(final_df["final_best_HVMinus"].median()),
        "final_best_HVMinus_q25": float(final_df["final_best_HVMinus"].quantile(0.25)),
        "final_best_HVMinus_q75": float(final_df["final_best_HVMinus"].quantile(0.75)),
    }
