"""Shared helpers for configuration-space analysis scripts."""

from __future__ import annotations

import math
from pathlib import Path

import numpy as np
import pandas as pd
from scipy import stats
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OrdinalEncoder, StandardScaler

from config import (
    BUDGETS,
    CATEGORICAL_COLUMNS,
    FAMILIES,
    FRONT_TYPES,
    NUMERICAL_COLUMNS,
    TABLES_DIR,
    TOP_PARAMETERS,
)
from data_loader import get_configuration_columns

ELITE_QUANTILES = [0.05, 0.10, 0.20]
CHECKPOINT_FRACTIONS = [0.10, 0.25, 0.50, 0.75, 1.00]
ELITE_VIEWS = ["EP", "HV", "joint"]
JOINT_VIEW = "joint"

BRANCHING_CATEGORICALS = [
    "archiveType",
    "mutation",
    "crossover",
    "algorithmResult",
    "createInitialSolutions",
    "selection",
    "mutationRepairStrategy",
    "crossoverRepairStrategy",
    "offspringPopulationSize",
]

REPRESENTATIVE_CATEGORICALS = [
    "crossover",
    "mutation",
    "selection",
    "createInitialSolutions",
]

REPRESENTATIVE_NUMERICALS = [
    "crossoverProbability",
    "mutationProbabilityFactor",
    "populationSizeWithArchive",
    "offspringPopulationSize",
    "levyFlightMutationBeta",
    "levyFlightMutationStepSize",
    "blxAlphaCrossoverAlpha",
    "blxAlphaBetaCrossoverAlpha",
    "blxAlphaBetaCrossoverBeta",
    "powerLawMutationDelta",
    "boltzmannTemperature",
]


def iter_slices() -> list[tuple[str, str, int]]:
    """Return all family/front/budget slices in a deterministic order."""
    return [
        (family, front_type, budget)
        for budget in BUDGETS
        for front_type in FRONT_TYPES
        for family in FAMILIES
    ]


def active_configuration_columns(df: pd.DataFrame) -> list[str]:
    """Return configuration columns present in a dataframe."""
    return [column for column in get_configuration_columns(df) if column in df.columns]


def parameter_kind(parameter: str) -> str:
    """Return whether a parameter is treated as numerical or categorical."""
    return "numerical" if parameter in NUMERICAL_COLUMNS else "categorical"


def add_joint_scores(
    df: pd.DataFrame, group_cols: list[str] | None = None
) -> pd.DataFrame:
    """Add normalized EP/HV ranks plus a joint score."""

    def _score_group(group: pd.DataFrame) -> pd.DataFrame:
        scored = group.copy()
        size = len(scored)
        if size == 0:
            return scored
        scored["ep_rank_pct"] = (
            scored["EP"].rank(method="average", ascending=True, pct=True).astype(float)
        )
        scored["hv_rank_pct"] = (
            scored["HVMinus"].rank(method="average", ascending=True, pct=True).astype(float)
        )
        scored["joint_score"] = 0.5 * (scored["ep_rank_pct"] + scored["hv_rank_pct"])
        scored["joint_rank"] = (
            scored["joint_score"]
            .rank(method="first", ascending=True)
            .astype(int)
        )
        return scored

    if not group_cols:
        return _score_group(df)

    parts = [_score_group(group) for _, group in df.groupby(group_cols, sort=False)]
    if not parts:
        return df.copy()
    return pd.concat(parts, ignore_index=False).sort_index()


def score_column_for_view(view: str) -> str:
    """Map a view name to the score column used for sorting."""
    if view == "EP":
        return "EP"
    if view == "HV":
        return "HVMinus"
    if view == JOINT_VIEW:
        return "joint_score"
    raise ValueError(f"Unknown view: {view}")


def elite_subset(df: pd.DataFrame, view: str, quantile: float) -> pd.DataFrame:
    """Return the elite subset for one view and fraction."""
    if view == JOINT_VIEW and "joint_score" not in df.columns:
        df = add_joint_scores(df)
    score_column = score_column_for_view(view)
    elite_size = max(1, int(math.ceil(len(df) * quantile)))
    order_columns = [score_column]
    for tie_column in ["EP", "HVMinus", "run", "SolutionId"]:
        if tie_column in df.columns and tie_column not in order_columns:
            order_columns.append(tie_column)
    elite = df.sort_values(order_columns, ascending=True).head(elite_size).copy()
    elite["elite_view"] = view
    elite["elite_quantile"] = quantile
    elite["elite_rank"] = np.arange(1, len(elite) + 1)
    return elite


def safe_category_label(value: object) -> str:
    """Convert a possibly-missing categorical value to a stable label."""
    if pd.isna(value):
        return "<NA>"
    return str(value)


def numeric_range(series: pd.Series) -> tuple[float | None, float | None, float | None]:
    """Return min, max and width for a numeric series."""
    values = pd.to_numeric(series, errors="coerce").dropna()
    if values.empty:
        return None, None, None
    lower = float(values.min())
    upper = float(values.max())
    return lower, upper, float(upper - lower)


def pairwise_gower(
    left: pd.DataFrame,
    right: pd.DataFrame,
    config_columns: list[str],
    pooled_ranges: dict[str, float],
    weights: dict[str, float] | None = None,
) -> np.ndarray:
    """Compute a mixed-type Gower distance matrix."""
    if weights is None:
        weights = {column: 1.0 for column in config_columns}

    total_weight = float(sum(weights.get(column, 0.0) for column in config_columns))
    if total_weight <= 0.0:
        raise ValueError("Total weight must be positive.")

    distances = np.zeros((len(left), len(right)), dtype=float)
    for column in config_columns:
        weight = float(weights.get(column, 0.0))
        if weight <= 0.0:
            continue

        if column in NUMERICAL_COLUMNS:
            left_values = pd.to_numeric(left[column], errors="coerce").to_numpy(dtype=float)
            right_values = pd.to_numeric(right[column], errors="coerce").to_numpy(dtype=float)
            left_valid = np.isfinite(left_values)
            right_valid = np.isfinite(right_values)
            one_missing = np.logical_xor.outer(~left_valid, ~right_valid).astype(float)
            both_present = np.outer(left_valid, right_valid)
            delta = one_missing

            column_range = pooled_ranges.get(column, 0.0)
            if column_range > 0.0:
                normalized = np.abs(np.subtract.outer(left_values, right_values)) / column_range
                delta = delta + np.where(both_present, normalized, 0.0)
        else:
            left_values = left[column].where(left[column].notna(), None).to_numpy(dtype=object)
            right_values = right[column].where(right[column].notna(), None).to_numpy(dtype=object)
            left_missing = np.equal(left_values, None)
            right_missing = np.equal(right_values, None)
            one_missing = np.logical_xor.outer(left_missing, right_missing).astype(float)
            both_present = np.outer(~left_missing, ~right_missing)
            different = np.not_equal.outer(left_values, right_values).astype(float)
            delta = one_missing + np.where(both_present, different, 0.0)

        distances += weight * delta

    return distances / total_weight


def mixed_normalization_ranges(
    df: pd.DataFrame, config_columns: list[str]
) -> dict[str, float]:
    """Return pooled numeric ranges for Gower normalization."""
    result: dict[str, float] = {}
    for column in config_columns:
        if column not in NUMERICAL_COLUMNS:
            continue
        _, _, width = numeric_range(df[column])
        result[column] = 0.0 if width is None else width
    return result


def medoid_index(distance_matrix: np.ndarray) -> int:
    """Return the index of the medoid row in a square distance matrix."""
    if distance_matrix.shape[0] != distance_matrix.shape[1]:
        raise ValueError("Medoid requires a square distance matrix.")
    return int(np.argmin(distance_matrix.mean(axis=1)))


def checkpoint_targets(
    evaluations: pd.Series | list[int], fractions: list[float] | None = None
) -> list[int]:
    """Map checkpoint fractions to the closest available evaluations."""
    fractions = fractions or CHECKPOINT_FRACTIONS
    available = sorted({int(value) for value in evaluations})
    if not available:
        return []
    max_eval = max(available)
    chosen: list[int] = []
    for fraction in fractions:
        target = int(round(max_eval * fraction))
        checkpoint = next((value for value in available if value >= target), available[-1])
        if checkpoint not in chosen:
            chosen.append(checkpoint)
    if available[-1] not in chosen:
        chosen.append(available[-1])
    return chosen


def prioritized_parameters(
    front_type: str,
    budget: int,
    available_columns: list[str],
    top_k: int = 8,
) -> list[str]:
    """Pick high-signal parameters for figures and rule models."""
    summary_path = (
        TABLES_DIR
        / "component_importance"
        / f"budget_{budget}"
        / f"component_importance_consensus_summary_{budget}.csv"
    )
    if summary_path.exists():
        summary = pd.read_csv(summary_path)
        summary = summary.loc[summary["front_type"] == front_type].copy()
        if not summary.empty:
            ranked = (
                summary.groupby("parameter_family")["consensus_importance"]
                .mean()
                .sort_values(ascending=False)
                .index.tolist()
            )
            ordered = [parameter for parameter in ranked if parameter in available_columns]
            if ordered:
                return ordered[:top_k]

    fallback = [
        *[parameter for parameter in TOP_PARAMETERS if parameter in available_columns],
        *[parameter for parameter in BRANCHING_CATEGORICALS if parameter in available_columns],
        *[parameter for parameter in available_columns if parameter not in TOP_PARAMETERS],
    ]
    deduped: list[str] = []
    for parameter in fallback:
        if parameter not in deduped:
            deduped.append(parameter)
    return deduped[:top_k]


def prioritized_branching_parameters(
    front_type: str,
    budget: int,
    available_columns: list[str],
    max_categoricals: int = 5,
    max_numericals: int = 4,
) -> tuple[list[str], list[str]]:
    """Select categorical and numerical parameters to feed rule extraction."""
    ranked = prioritized_parameters(front_type, budget, available_columns, top_k=len(available_columns))
    categorical = [
        parameter
        for parameter in ranked
        if parameter_kind(parameter) == "categorical"
    ]
    numerical = [
        parameter
        for parameter in ranked
        if parameter_kind(parameter) == "numerical"
    ]

    categorical = [
        parameter
        for parameter in [*categorical, *BRANCHING_CATEGORICALS]
        if parameter in available_columns and parameter_kind(parameter) == "categorical"
    ]
    numerical = [
        parameter
        for parameter in [*numerical, *TOP_PARAMETERS]
        if parameter in available_columns and parameter_kind(parameter) == "numerical"
    ]

    categorical = list(dict.fromkeys(categorical))[:max_categoricals]
    numerical = list(dict.fromkeys(numerical))[:max_numericals]
    return categorical, numerical


def report_table_path(*parts: str) -> Path:
    """Build a path under scripts/analysis/tables/report."""
    return TABLES_DIR / "report" / Path(*parts)


def iter_family_front_pairs() -> list[tuple[str, str]]:
    """Return all family/front pairs in deterministic order."""
    return [
        (family, front_type)
        for front_type in FRONT_TYPES
        for family in FAMILIES
    ]


def configuration_columns_by_type(
    df: pd.DataFrame, columns: list[str] | None = None
) -> tuple[list[str], list[str]]:
    """Split present configuration columns into categorical and numerical."""
    columns = columns or active_configuration_columns(df)
    categorical = [
        column
        for column in columns
        if column in CATEGORICAL_COLUMNS and column in df.columns and df[column].notna().any()
    ]
    numerical = [
        column
        for column in columns
        if column in NUMERICAL_COLUMNS and column in df.columns and pd.to_numeric(df[column], errors="coerce").notna().any()
    ]
    return categorical, numerical


def build_model_preprocessor(
    df: pd.DataFrame, columns: list[str] | None = None
) -> tuple[ColumnTransformer, list[str]]:
    """Create a reusable sklearn preprocessor for mixed configuration data."""
    categorical, numerical = configuration_columns_by_type(df, columns)
    transformers: list[tuple[str, object, list[str]]] = []

    if categorical:
        categorical_pipeline = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="constant", fill_value="<NA>")),
                (
                    "encoder",
                    OrdinalEncoder(
                        handle_unknown="use_encoded_value",
                        unknown_value=-1,
                        encoded_missing_value=-1,
                    ),
                ),
            ]
        )
        transformers.append(("categorical", categorical_pipeline, categorical))

    if numerical:
        numerical_pipeline = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="constant", fill_value=-1.0)),
                ("scaler", StandardScaler()),
            ]
        )
        transformers.append(("numerical", numerical_pipeline, numerical))

    preprocessor = ColumnTransformer(
        transformers=transformers,
        remainder="drop",
        sparse_threshold=0.0,
    )
    feature_names = [*categorical, *numerical]
    return preprocessor, feature_names


def mode_value(series: pd.Series) -> object:
    """Return a deterministic mode value."""
    non_missing = series.dropna()
    if non_missing.empty:
        return np.nan
    modes = non_missing.mode(dropna=True)
    if modes.empty:
        return np.nan
    return modes.astype(str).sort_values().iloc[0] if non_missing.dtype == object else modes.sort_values().iloc[0]


def vargha_delaney_a(left: pd.Series, right: pd.Series) -> float:
    """Compute the Vargha-Delaney A effect size for two independent samples."""
    left_values = pd.to_numeric(left, errors="coerce").dropna()
    right_values = pd.to_numeric(right, errors="coerce").dropna()
    if left_values.empty or right_values.empty:
        return float("nan")
    u_stat, _ = stats.mannwhitneyu(left_values, right_values, alternative="two-sided")
    return float(u_stat / (len(left_values) * len(right_values)))


def effect_size_label(a_stat: float) -> str:
    """Map Vargha-Delaney A to a qualitative label."""
    if not np.isfinite(a_stat):
        return "NA"
    magnitude = abs(a_stat - 0.5)
    if magnitude <= 0.06:
        return "negligible"
    if magnitude <= 0.14:
        return "small"
    if magnitude <= 0.21:
        return "medium"
    return "large"


def representative_core_modes(
    df: pd.DataFrame, categorical_columns: list[str] | None = None
) -> dict[str, object]:
    """Return the modal categorical combination used to define a representative subset."""
    categorical_columns = categorical_columns or REPRESENTATIVE_CATEGORICALS
    result: dict[str, object] = {}
    for column in categorical_columns:
        if column in df.columns:
            result[column] = mode_value(df[column])
    return result


def filter_by_modes(df: pd.DataFrame, mode_map: dict[str, object]) -> pd.DataFrame:
    """Filter rows that match a complete categorical modal combination."""
    filtered = df.copy()
    for column, value in mode_map.items():
        if pd.isna(value):
            filtered = filtered.loc[filtered[column].isna()]
        else:
            filtered = filtered.loc[filtered[column] == value]
    return filtered.copy()


def build_representative_configuration(
    df: pd.DataFrame,
    categorical_focus: list[str] | None = None,
) -> tuple[dict[str, object], pd.DataFrame]:
    """Build a representative configuration summary and the matching subset."""
    config_columns = active_configuration_columns(df)
    categorical_focus = categorical_focus or REPRESENTATIVE_CATEGORICALS
    mode_map = representative_core_modes(df, categorical_focus)
    subset = filter_by_modes(df, mode_map)

    configuration: dict[str, object] = {}
    for column in config_columns:
        if column in CATEGORICAL_COLUMNS:
            configuration[column] = mode_value(subset[column]) if column in subset.columns else np.nan
        else:
            values = pd.to_numeric(subset[column], errors="coerce").dropna()
            configuration[column] = float(values.median()) if not values.empty else np.nan

    summary = {
        "matching_rows": int(len(subset)),
        "coverage_pct": float(100.0 * len(subset) / len(df)) if len(df) else float("nan"),
        "hv_median": float((-subset["HVMinus"]).median()) if "HVMinus" in subset.columns and not subset.empty else float("nan"),
        "ep_median": float(subset["EP"].median()) if "EP" in subset.columns and not subset.empty else float("nan"),
        **{f"mode_{column}": value for column, value in mode_map.items()},
        **configuration,
    }
    return summary, subset


def format_configuration_value(value: object) -> str:
    """Format a configuration value for CLI-style output."""
    if pd.isna(value):
        return ""
    if isinstance(value, (int, np.integer)):
        return str(int(value))
    if isinstance(value, (float, np.floating)):
        rounded = round(float(value), 6)
        if rounded.is_integer():
            return str(int(rounded))
        return f"{rounded:.6f}".rstrip("0").rstrip(".")
    return str(value)


def configuration_to_cli_lines(configuration: dict[str, object]) -> list[str]:
    """Render a configuration dictionary as ``--key value`` lines."""
    lines: list[str] = []
    for key, value in configuration.items():
        rendered = format_configuration_value(value)
        if rendered:
            lines.append(f"--{key} {rendered}")
    return lines
