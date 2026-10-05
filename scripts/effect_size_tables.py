"""Effect size tables (Vargha-Delaney A12) of a jMetal validation study.

A Wilcoxon test says whether a difference is significant, not how large it is: with enough runs
and small dispersions, tiny differences are significant. The A12 statistic of Vargha and Delaney
(2000) measures the size: the probability that a run of the pivot algorithm is better than a run of
the other one (ties count one half). 0.5 means no difference; the further from 0.5, the larger the
effect. The usual thresholds on |A12 - 0.5| are 0.06 (small), 0.14 (medium) and 0.21 (large), that
is, A12 = 0.56, 0.64 and 0.71 (or 0.44, 0.36 and 0.29 when the pivot is worse).

For each indicator it writes ``A12_<indicator>.csv``, with the A12 of the pivot against every other
algorithm on every problem, and prints it with the magnitude of each value.

Example:
    python scripts/effect_size_tables.py \\
        results/tutorial-e9/validation/QualityIndicatorSummary.csv \\
        --pivot NSGAIIWFG --indicators HV,IGD+ --output-dir results/tutorial-e9/tables
"""

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from study_summary import indicators_of, is_maximized, load_summary

THRESHOLDS = [(0.21, "large"), (0.14, "medium"), (0.06, "small")]


def a12(pivot: np.ndarray, other: np.ndarray, maximize: bool) -> float:
    """Probability that a value of ``pivot`` is better than one of ``other`` (ties count 1/2)."""
    pivot = np.asarray(pivot, dtype=float)[:, None]
    other = np.asarray(other, dtype=float)[None, :]
    better = (pivot > other) if maximize else (pivot < other)
    ties = pivot == other
    return float((better.sum() + 0.5 * ties.sum()) / (pivot.size * other.size))


def magnitude(value: float) -> str:
    """The magnitude of an A12 value: negligible, small, medium or large."""
    # Rounded, so that 0.71 is as far from 0.5 as the threshold 0.21 despite floating point
    distance = round(abs(value - 0.5), 10)
    for threshold, label in THRESHOLDS:
        if distance >= threshold:
            return label
    return "negligible"


def a12_table(data: pd.DataFrame, indicator: str, pivot: str) -> pd.DataFrame:
    """A12 of the pivot against every other algorithm (columns), on every problem (rows)."""
    rows = data[data["IndicatorName"] == indicator]
    algorithms = [a for a in dict.fromkeys(rows["Algorithm"]) if a != pivot]
    problems = list(dict.fromkeys(rows["Problem"]))
    table = pd.DataFrame(index=problems, columns=algorithms, dtype=float)
    for problem in problems:
        values = rows[rows["Problem"] == problem]
        pivot_values = values[values["Algorithm"] == pivot]["IndicatorValue"].to_numpy()
        for algorithm in algorithms:
            other_values = values[values["Algorithm"] == algorithm]["IndicatorValue"].to_numpy()
            table.loc[problem, algorithm] = a12(
                pivot_values, other_values, is_maximized(indicator)
            )
    return table


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Vargha-Delaney A12 tables of a jMetal QualityIndicatorSummary.csv"
    )
    parser.add_argument("summary", type=Path, help="jMetal's QualityIndicatorSummary.csv")
    parser.add_argument("--pivot", required=True, help="algorithm compared with the others")
    parser.add_argument(
        "--algorithms", help="comma-separated algorithms to include (default: all in the file)"
    )
    parser.add_argument(
        "--indicators", help="comma-separated indicators (default: all in the file)"
    )
    parser.add_argument("--output-dir", type=Path, default=Path("."))
    args = parser.parse_args()

    algorithms = args.algorithms.split(",") if args.algorithms else None
    data = load_summary(args.summary, algorithms)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for indicator in indicators_of(data, args.indicators):
        table = a12_table(data, indicator, args.pivot)
        csv_file = args.output_dir / f"A12_{indicator}.csv"
        table.to_csv(csv_file, float_format="%.3f")
        labelled = table.apply(lambda column: column.map(lambda v: f"{v:.2f} {magnitude(v)}"))
        print(f"A12 of {args.pivot} against each algorithm, {indicator}:")
        print(labelled.to_string())
        print(f"Written {csv_file}\n")


if __name__ == "__main__":
    main()
