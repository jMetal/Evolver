"""Wilcoxon pivot tables of a jMetal validation study, generated with SAES.

Reads the ``QualityIndicatorSummary.csv`` written by jMetal's ``ComputeQualityIndicators`` and,
for each quality indicator, writes a table with the median and interquartile range of every
algorithm on every problem. The pivot algorithm (usually the tuned configuration) goes in the last
column, and each other cell is marked with the result of the Wilcoxon rank-sum test (Mann-Whitney
U, significance level 0.05) against it: ``+`` the pivot is better, ``-`` the pivot is worse, ``=`` no
significant difference. The last row counts those results per algorithm. The best and second-best
medians of each problem are shaded dark and light gray.

The tables are produced by SAES (https://github.com/jMetal/SAES), which must be installed
(``pip install SAES``). Each one is written as a LaTeX document (``WilcoxonPivot_<indicator>.tex``)
and, with ``--png``, also as an image (``WilcoxonPivot_<indicator>.png``), which needs
``pdflatex`` and ``pdftoppm``.

Example:
    python scripts/wilcoxon_pivot_tables.py \\
        results/tutorial-training-sets/validation/QualityIndicatorSummary.csv \\
        --pivot NSGAIIDTLZ --order NSGAII,NSGAIII,SMSEMOA,AGEMOEA,NSGAIIDTLZ \\
        --output-dir results/tutorial-training-sets/tables --png
"""

import argparse
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import pandas as pd

# jMetal indicator names that are maximized; every other indicator is minimized.
MAXIMIZED_INDICATORS = {"HV", "NHV+"}

STANDALONE_PREAMBLE = r"""\documentclass[border=6pt]{standalone}
\usepackage{colortbl}
\usepackage[table]{xcolor}
\usepackage{siunitx}
\sisetup{output-exponent-marker=\text{e}}
\xdefinecolor{gray95}{gray}{0.65}
\xdefinecolor{gray25}{gray}{0.8}
\begin{document}
"""


def to_saes_format(summary: pd.DataFrame, order: list[str] | None) -> pd.DataFrame:
    """Renames jMetal's columns to SAES's, and sorts the algorithms in the given order."""
    data = summary.rename(
        columns={
            "Problem": "Instance",
            "IndicatorName": "MetricName",
            "IndicatorValue": "MetricValue",
        }
    )
    if order:
        unknown = set(order) - set(data["Algorithm"])
        if unknown:
            sys.exit(f"Unknown algorithms in --order: {sorted(unknown)}")
        data = data[data["Algorithm"].isin(order)].copy()
        data["Algorithm"] = pd.Categorical(data["Algorithm"], categories=order, ordered=True)
        data = data.sort_values(["Algorithm"], kind="stable")
        data["Algorithm"] = data["Algorithm"].astype(str)
    return data


def write_png(latex_document: str, png_file: Path) -> None:
    """Compiles the tabular of an SAES LaTeX document on its own, and converts it to PNG."""
    match = re.search(r"\\begin\{tabular\}.*\\end\{tabular\}", latex_document, re.DOTALL)
    if match is None:
        sys.exit("No tabular found in the SAES table")
    with tempfile.TemporaryDirectory() as tmp:
        tex_file = Path(tmp) / "table.tex"
        tex_file.write_text(STANDALONE_PREAMBLE + match.group(0) + "\n\\end{document}\n")
        subprocess.run(
            ["pdflatex", "-interaction=nonstopmode", tex_file.name],
            cwd=tmp,
            check=True,
            stdout=subprocess.DEVNULL,
        )
        subprocess.run(
            ["pdftoppm", "-png", "-r", "200", "-singlefile", "table.pdf", "table"],
            cwd=tmp,
            check=True,
        )
        shutil.copy(Path(tmp) / "table.png", png_file)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Wilcoxon pivot tables (SAES) of a jMetal QualityIndicatorSummary.csv"
    )
    parser.add_argument("summary", type=Path, help="jMetal's QualityIndicatorSummary.csv")
    parser.add_argument("--pivot", required=True, help="algorithm in the last column")
    parser.add_argument(
        "--order", help="comma-separated order of the algorithms (default: as in the file)"
    )
    parser.add_argument(
        "--indicators", help="comma-separated indicators (default: all in the file)"
    )
    parser.add_argument("--output-dir", type=Path, default=Path("."))
    parser.add_argument("--png", action="store_true", help="also write each table as a PNG")
    args = parser.parse_args()

    try:
        from SAES.latex_generation.stats_table import WilcoxonPivot
    except ImportError:
        sys.exit("SAES is not installed: pip install SAES")

    order = args.order.split(",") if args.order else None
    data = to_saes_format(pd.read_csv(args.summary), order)
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
        table = WilcoxonPivot(data, metrics, indicator, pivot=args.pivot)
        table.create_latex_table()
        # SAES writes the counts as \textbf20, which only makes the first digit bold
        latex_document = re.sub(r"\\textbf(\d+)", r"\\textbf{\1}", table.latex_doc)
        tex_file = args.output_dir / f"WilcoxonPivot_{indicator}.tex"
        tex_file.write_text(latex_document)
        print(f"Written {tex_file}")
        if args.png:
            png_file = args.output_dir / f"WilcoxonPivot_{indicator}.png"
            write_png(latex_document, png_file)
            print(f"Written {png_file}")


if __name__ == "__main__":
    main()
