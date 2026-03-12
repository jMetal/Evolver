"""Utilities to load and summarize convergence traces from Evolver results."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

from config import (
    CATEGORICAL_COLUMNS,
    CATEGORICAL_DECODINGS,
    NUMERICAL_COLUMNS,
    RESULTS_ROOT,
    resolve_results_root,
)

REQUIRED_COLUMNS = {"Evaluation", "SolutionId", "EP", "HVMinus"}
CONFIG_EXCLUDED_COLUMNS = {
    "EP",
    "HVMinus",
    "Evaluation",
    "SolutionId",
    "run",
    "family",
    "front_type",
    "budget",
    "scope",
    "pooled_run_id",
    "config_signature",
    "rank",
    "rows",
    "runs",
    "row_share_pct",
    "run_share_pct",
    "ep_median",
    "hv_median",
    "consensus_rank",
    "consensus_rows",
    "consensus_runs",
    "consensus_row_share_pct",
    "consensus_run_share_pct",
    "mean_row_share_pct",
    "mean_run_share_pct",
    "rows_RE3D",
    "rows_RWA3D",
    "runs_RE3D",
    "runs_RWA3D",
    "row_share_pct_RE3D",
    "row_share_pct_RWA3D",
    "run_share_pct_RE3D",
    "run_share_pct_RWA3D",
    "ep_median_RE3D",
    "ep_median_RWA3D",
    "hv_median_RE3D",
    "hv_median_RWA3D",
    "is_best_EP",
    "is_best_HV",
    "row_id",
    "trace_row_id",
    "ep_rank_pct",
    "hv_rank_pct",
    "joint_score",
    "joint_rank",
    "elite_view",
    "elite_quantile",
    "elite_rank",
    "checkpoint_eval",
    "checkpoint_fraction",
    "incumbent_found_at",
}


def experiment_path(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> Path:
    """Return the directory for one family/front/budget combination."""
    root = resolve_results_root(front_type, results_root if results_root is not None else RESULTS_ROOT)
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


def load_configurations(
    family: str,
    front_type: str,
    budget: int,
    run: int,
    results_root: Path | None = None,
) -> pd.DataFrame:
    """Load one CONFIGURATIONS.csv file for an experiment run."""
    path = experiment_path(family, front_type, budget, results_root) / f"run{run}" / "CONFIGURATIONS.csv"
    return pd.read_csv(path).drop_duplicates(
        subset=["Evaluation", "SolutionId"], keep="first"
    )


def decode_categoricals(df: pd.DataFrame) -> pd.DataFrame:
    """Map encoded categorical values to readable labels."""
    decoded = df.copy()
    for column, mapping in CATEGORICAL_DECODINGS.items():
        if column in decoded.columns:
            decoded[column] = decoded[column].map(mapping).fillna(decoded[column])
    return decoded


def load_run_with_config(
    family: str,
    front_type: str,
    budget: int,
    run: int,
    results_root: Path | None = None,
) -> pd.DataFrame:
    """Merge indicators and configuration values for one run."""
    indicators = load_indicators(family, front_type, budget, run, results_root)
    configurations = load_configurations(family, front_type, budget, run, results_root)
    merged = indicators.merge(
        configurations, on=["Evaluation", "SolutionId"], how="inner"
    )
    return decode_categoricals(merged)


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


def load_all_runs_with_config(
    family: str, front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Load and concatenate all runs with configuration columns included."""
    frames: list[pd.DataFrame] = []
    for run in available_runs(family, front_type, budget, results_root):
        merged = load_run_with_config(family, front_type, budget, run, results_root).copy()
        merged["run"] = run
        frames.append(merged)
    return pd.concat(frames, ignore_index=True)


def load_runs_for_families(
    families: list[str], front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Load one budget/front slice pooling several families."""
    frames: list[pd.DataFrame] = []
    for family in families:
        indicators = load_all_runs(family, front_type, budget, results_root).copy()
        indicators["family"] = family
        indicators["pooled_run_id"] = (
            indicators["family"] + "_run" + indicators["run"].astype(str)
        )
        frames.append(indicators)
    return pd.concat(frames, ignore_index=True)


def load_runs_with_config_for_families(
    families: list[str], front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Load one budget/front slice pooling several families with configs."""
    frames: list[pd.DataFrame] = []
    for family in families:
        merged = load_all_runs_with_config(family, front_type, budget, results_root).copy()
        merged["family"] = family
        merged["pooled_run_id"] = merged["family"] + "_run" + merged["run"].astype(str)
        frames.append(merged)
    return pd.concat(frames, ignore_index=True)


def best_per_evaluation(indicators: pd.DataFrame, run_col: str = "run") -> pd.DataFrame:
    """Keep the best EP and HVMinus found at each evaluation and run."""
    return (
        indicators.groupby([run_col, "Evaluation"], as_index=False)
        .agg(best_EP=("EP", "min"), best_HVMinus=("HVMinus", "min"))
        .sort_values([run_col, "Evaluation"])
    )


def get_final_configs(indicators: pd.DataFrame, run_col: str = "run") -> pd.DataFrame:
    """Extract rows at the last evaluation of each run."""
    last_eval = indicators.groupby(run_col, as_index=False)["Evaluation"].max()
    return indicators.merge(last_eval, on=[run_col, "Evaluation"], how="inner")


def best_config_per_evaluation(
    indicators: pd.DataFrame, run_col: str = "run"
) -> pd.DataFrame:
    """Keep the best configuration row at each evaluation and run.

    Best is defined by lower ``HVMinus`` (equivalently higher HV), using ``EP`` as
    tie-breaker and ``SolutionId`` as a stable final fallback.
    """
    order_columns = [run_col, "Evaluation", "HVMinus", "EP"]
    if "SolutionId" in indicators.columns:
        order_columns.append("SolutionId")
    ordered = indicators.sort_values(order_columns, ascending=True)
    return (
        ordered.groupby([run_col, "Evaluation"], as_index=False)
        .head(1)
        .reset_index(drop=True)
    )


def get_final_best_configs(
    indicators: pd.DataFrame, run_col: str = "run"
) -> pd.DataFrame:
    """Extract one final best configuration row per run."""
    best = best_config_per_evaluation(indicators, run_col=run_col)
    last_eval = best.groupby(run_col, as_index=False)["Evaluation"].max()
    return best.merge(last_eval, on=[run_col, "Evaluation"], how="inner")


def encode_config_vector(df: pd.DataFrame) -> tuple[pd.DataFrame, list[str]]:
    """One-hot encode categoricals and min-max normalize numerical parameters."""
    parts: list[pd.DataFrame] = []

    for column in CATEGORICAL_COLUMNS:
        if column in df.columns and column != "variation":
            dummies = pd.get_dummies(df[column], prefix=column).astype(float)
            if not dummies.empty:
                parts.append(dummies)

    for column in NUMERICAL_COLUMNS:
        if column in df.columns:
            values = df[column].fillna(0.0).astype(float)
            value_min = values.min()
            value_max = values.max()
            if value_max > value_min:
                values = (values - value_min) / (value_max - value_min)
            parts.append(pd.DataFrame({column: values}, index=df.index))

    if not parts:
        raise ValueError("No configuration columns available to encode.")

    feature_df = pd.concat(parts, axis=1).fillna(0.0)
    return feature_df, feature_df.columns.tolist()


def get_configuration_columns(df: pd.DataFrame) -> list[str]:
    """Return the columns that define the base-level configuration."""
    return [column for column in df.columns if column not in CONFIG_EXCLUDED_COLUMNS]


def build_config_signature(df: pd.DataFrame, decimals: int = 6) -> pd.Series:
    """Create a stable string signature for each configuration row."""
    signature = df[get_configuration_columns(df)].copy()
    for column in signature.columns:
        if pd.api.types.is_numeric_dtype(signature[column]):
            signature[column] = signature[column].round(decimals)
        else:
            signature[column] = signature[column].fillna("NA").astype(str)
    return signature.astype(str).agg("|".join, axis=1)


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


def pooled_convergence_summary(
    families: list[str], front_type: str, budget: int, results_root: Path | None = None
) -> pd.DataFrame:
    """Compute median and IQR envelopes pooling several families together."""
    best = best_per_evaluation(
        load_runs_for_families(families, front_type, budget, results_root),
        run_col="pooled_run_id",
    )
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
