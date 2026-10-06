"""Boxplots of a jMetal validation study, generated with SAES.

A table of medians and interquartile ranges summarizes each distribution of runs with two numbers;
a boxplot shows the whole distribution: the box spans the interquartile range, the line inside it
is the median, the whiskers reach the most extreme values within 1.5 times the interquartile range,
and the points beyond them are outliers. It reveals what the medians hide, such as runs stuck in a
local front or a bimodal distribution.

For each indicator it writes ``Boxplot_<indicator>.png``, with one panel per problem.

The plots are produced by SAES (https://github.com/jMetal/SAES), which must be installed
(``pip install SAES``).

Example:
    python scripts/boxplots.py results/tutorial-validation/validation/QualityIndicatorSummary.csv \\
        --problems DTLZ1,DTLZ3,WFG1,WFG8 --algorithms NSGAII,SMPSO,NSGAIIWFG \\
        --indicators HV --output-dir results/tutorial-validation/tables
"""

import argparse
import sys
from pathlib import Path

from study_summary import indicators_of, load_summary, saes_metrics, to_saes_format


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Boxplots (SAES) of a jMetal QualityIndicatorSummary.csv"
    )
    parser.add_argument("summary", type=Path, help="jMetal's QualityIndicatorSummary.csv")
    parser.add_argument(
        "--problems", help="comma-separated problems to plot (default: all in the file)"
    )
    parser.add_argument(
        "--algorithms", help="comma-separated algorithms, in order (default: all in the file)"
    )
    parser.add_argument(
        "--indicators", help="comma-separated indicators (default: all in the file)"
    )
    parser.add_argument("--width", type=int, default=12, help="width of the figure, in inches")
    parser.add_argument("--output-dir", type=Path, default=Path("."))
    args = parser.parse_args()

    try:
        from SAES.plots.boxplot import Boxplot
    except ImportError:
        sys.exit("SAES is not installed: pip install SAES")

    algorithms = args.algorithms.split(",") if args.algorithms else None
    data = load_summary(args.summary, algorithms)
    if args.problems:
        problems = args.problems.split(",")
        unknown = set(problems) - set(data["Problem"])
        if unknown:
            sys.exit(f"Unknown problems: {sorted(unknown)}")
        data = data[data["Problem"].isin(problems)]
    indicators = indicators_of(data, args.indicators)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    saes_data = to_saes_format(data)
    for indicator in indicators:
        file_name = f"Boxplot_{indicator}.png"
        boxplot = Boxplot(saes_data, saes_metrics(indicators), indicator)
        boxplot.save_all_instances(str(args.output_dir), file_name, width=args.width)
        print(f"Written {args.output_dir / file_name}")


if __name__ == "__main__":
    main()
