"""Critical difference plots of a jMetal validation study, generated with SAES.

Reads the ``QualityIndicatorSummary.csv`` written by jMetal's ``ComputeQualityIndicators`` and,
for each quality indicator, draws a critical difference plot (Demšar, 2006): the algorithms are
placed by their average Friedman rank over the problems (computed on the median of each problem),
and those whose ranks differ by less than the Nemenyi critical difference (alpha = 0.05) are
joined by a bar, meaning that their differences are not significant.

The plots are produced by SAES (https://github.com/jMetal/SAES), which must be installed
(``pip install SAES``).

Example:
    python scripts/critical_difference_plots.py \\
        results/tutorial-training-sets/validation/QualityIndicatorSummary.csv \\
        --indicators HV,IGD+ --output-dir results/tutorial-training-sets/tables
"""

import argparse
import sys
from pathlib import Path

import pandas as pd

# jMetal indicator names that are maximized; every other indicator is minimized.
MAXIMIZED_INDICATORS = {"HV", "NHV+"}


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Critical difference plots (SAES) of a jMetal QualityIndicatorSummary.csv"
    )
    parser.add_argument("summary", type=Path, help="jMetal's QualityIndicatorSummary.csv")
    parser.add_argument(
        "--algorithms", help="comma-separated algorithms to include (default: all in the file)"
    )
    parser.add_argument(
        "--indicators", help="comma-separated indicators (default: all in the file)"
    )
    parser.add_argument("--output-dir", type=Path, default=Path("."))
    args = parser.parse_args()

    try:
        from SAES.plots.cdplot import CDplot
    except ImportError:
        sys.exit("SAES is not installed: pip install SAES")

    data = pd.read_csv(args.summary).rename(
        columns={
            "Problem": "Instance",
            "IndicatorName": "MetricName",
            "IndicatorValue": "MetricValue",
        }
    )
    if args.algorithms:
        algorithms = args.algorithms.split(",")
        unknown = set(algorithms) - set(data["Algorithm"])
        if unknown:
            sys.exit(f"Unknown algorithms: {sorted(unknown)}")
        data = data[data["Algorithm"].isin(algorithms)]
    indicators = (
        args.indicators.split(",") if args.indicators else list(data["MetricName"].unique())
    )
    metrics = pd.DataFrame(
        {
            "MetricName": indicators,
            "Maximize": [indicator in MAXIMIZED_INDICATORS for indicator in indicators],
        }
    )

    args.output_dir.mkdir(parents=True, exist_ok=True)
    for indicator in indicators:
        file_name = f"CDplot_{indicator}.png"
        CDplot(data[data["MetricName"] == indicator], metrics, indicator).save(
            str(args.output_dir), file_name
        )
        print(f"Written {args.output_dir / file_name}")


if __name__ == "__main__":
    main()
