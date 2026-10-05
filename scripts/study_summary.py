"""Reading of the ``QualityIndicatorSummary.csv`` of a jMetal validation study.

Shared by the statistical analysis scripts (``boxplots.py``, ``effect_size_tables.py``,
``friedman_holm_tables.py`` and ``bayesian_plots.py``). The file, written by jMetal's
``ComputeQualityIndicators``, has one row per algorithm, problem, indicator and run, with the
columns ``Algorithm,Problem,IndicatorName,ExecutionId,IndicatorValue``.
"""

import sys
from pathlib import Path

import pandas as pd

# jMetal indicator names that are maximized; every other indicator is minimized.
MAXIMIZED_INDICATORS = {"HV", "NHV+"}


def is_maximized(indicator: str) -> bool:
    return indicator in MAXIMIZED_INDICATORS


def load_summary(summary: Path, algorithms: list[str] | None = None) -> pd.DataFrame:
    """Reads the summary, keeping only the given algorithms (all if None), in that order."""
    data = pd.read_csv(summary)
    if algorithms:
        unknown = set(algorithms) - set(data["Algorithm"])
        if unknown:
            sys.exit(f"Unknown algorithms: {sorted(unknown)}")
        data = data[data["Algorithm"].isin(algorithms)].copy()
        data["Algorithm"] = pd.Categorical(data["Algorithm"], categories=algorithms, ordered=True)
        data = data.sort_values("Algorithm", kind="stable")
        data["Algorithm"] = data["Algorithm"].astype(str)
    return data


def indicators_of(data: pd.DataFrame, indicators: str | None) -> list[str]:
    """The comma-separated indicators given, or all those in the data."""
    return indicators.split(",") if indicators else list(data["IndicatorName"].unique())


def to_saes_format(data: pd.DataFrame) -> pd.DataFrame:
    """Renames jMetal's columns to SAES's."""
    return data.rename(
        columns={
            "Problem": "Instance",
            "IndicatorName": "MetricName",
            "IndicatorValue": "MetricValue",
        }
    )


def saes_metrics(indicators: list[str]) -> pd.DataFrame:
    """The metrics table SAES needs: each indicator and whether it is maximized."""
    return pd.DataFrame(
        {
            "MetricName": indicators,
            "Maximize": [is_maximized(indicator) for indicator in indicators],
        }
    )


def median_table(data: pd.DataFrame, indicator: str) -> pd.DataFrame:
    """The median of each algorithm (columns) on each problem (rows), for one indicator."""
    rows = data[data["IndicatorName"] == indicator]
    algorithms = list(dict.fromkeys(rows["Algorithm"]))
    problems = list(dict.fromkeys(rows["Problem"]))
    table = rows.pivot_table(
        index="Problem", columns="Algorithm", values="IndicatorValue", aggfunc="median"
    )
    return table.loc[problems, algorithms]
