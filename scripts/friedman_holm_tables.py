"""Friedman test with Holm's post-hoc procedure of a jMetal validation study, computed with SAES.

The Friedman test compares several algorithms over a set of problems: on each problem the
algorithms are ranked by their median (1 = best), and the test checks whether their average ranks
differ more than chance would explain. If they do, the post-hoc procedure compares a control
algorithm (usually the tuned configuration) with each of the others, and Holm's procedure adjusts
the p-values of those comparisons, so that the probability of any false positive among them stays
below the significance level (García et al., 2010).

For each indicator it prints the average ranks, the Friedman statistic and p-value, and the
adjusted p-values of the control against every other algorithm, and writes them to
``FriedmanHolm_<indicator>.csv``.

The Friedman test is computed with SciPy (``scipy.stats.friedmanchisquare``; SAES's ``friedman``
miscomputes the statistic), and the post-hoc procedure with SAES (https://github.com/jMetal/SAES),
which must be installed (``pip install SAES``).

Example:
    python scripts/friedman_holm_tables.py \\
        results/tutorial-validation/validation/QualityIndicatorSummary.csv \\
        --control NSGAIIWFG --indicators HV,IGD+ --output-dir results/tutorial-validation/tables
"""

import argparse
import sys
from pathlib import Path

import pandas as pd

from study_summary import indicators_of, is_maximized, load_summary, median_table


def average_ranks(medians: pd.DataFrame, maximize: bool) -> pd.Series:
    """The average rank of each algorithm over the problems (1 = best; ties share their rank)."""
    ranks = medians.rank(axis=1, ascending=not maximize, method="average")
    return ranks.mean(axis=0)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Friedman test and Holm post-hoc (SAES) of a jMetal QualityIndicatorSummary.csv"
    )
    parser.add_argument("summary", type=Path, help="jMetal's QualityIndicatorSummary.csv")
    parser.add_argument("--control", required=True, help="algorithm compared with the others")
    parser.add_argument(
        "--algorithms", help="comma-separated algorithms to include (default: all in the file)"
    )
    parser.add_argument(
        "--indicators", help="comma-separated indicators (default: all in the file)"
    )
    parser.add_argument("--output-dir", type=Path, default=Path("."))
    args = parser.parse_args()

    try:
        from SAES.statistical_tests.apv_procedures import friedman_ph_test
    except ImportError:
        sys.exit("SAES is not installed: pip install SAES")
    from scipy.stats import friedmanchisquare

    algorithms = args.algorithms.split(",") if args.algorithms else None
    data = load_summary(args.summary, algorithms)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for indicator in indicators_of(data, args.indicators):
        maximize = is_maximized(indicator)
        medians = median_table(data, indicator)
        if args.control not in medians.columns:
            sys.exit(f"Unknown control algorithm: {args.control}")
        ranks = average_ranks(medians, maximize)
        statistic, p_value = friedmanchisquare(*(medians[column] for column in medians.columns))
        _, unadjusted, holm = friedman_ph_test(
            medians, maximize, control=args.control, apv_procedure="Holm"
        )

        rows = []
        for comparison, adjusted in holm["Holm"].items():
            other = comparison.split(" vs ")[1]
            rows.append(
                {
                    "Algorithm": other,
                    "AverageRank": ranks[other],
                    "PValue": unadjusted.loc[args.control, other],
                    "HolmPValue": adjusted,
                }
            )
        table = pd.DataFrame(rows).sort_values("AverageRank")
        csv_file = args.output_dir / f"FriedmanHolm_{indicator}.csv"
        table.to_csv(csv_file, index=False, float_format="%.4g")

        print(f"{indicator} ({len(medians)} problems, {len(medians.columns)} algorithms)")
        print("Average ranks: " + ", ".join(f"{a} {r:.2f}" for a, r in ranks.sort_values().items()))
        print(f"Friedman statistic {statistic:.3f}, p-value {p_value:.3g}")
        print(f"{args.control} against each algorithm (Holm's adjusted p-values):")
        print(table.to_string(index=False, float_format=lambda v: f"{v:.4g}"))
        print(f"Written {csv_file}\n")


if __name__ == "__main__":
    main()
