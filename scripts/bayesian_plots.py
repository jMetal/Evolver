"""Bayesian comparison of a pivot algorithm with the others in a jMetal validation study.

A p-value answers "how surprising would these results be if the algorithms were equivalent?"; the
Bayesian sign test (Benavoli et al., 2017) answers the question one usually wants to ask: "what is
the probability that one algorithm is better than the other, or that they are practically
equivalent?". On each problem, the difference of the medians of the two algorithms counts as a win
of one of them, or as a tie when it falls inside the region of practical equivalence (ROPE,
[-rope, rope], in the units of the indicator). The test turns those counts into a posterior
distribution over the three probabilities, and reports how often each outcome is the most probable.

The ROPE is a decision of the analyst: the largest difference that does not matter in practice. The
result depends on it, so it must be chosen for the indicator and stated with the results.

For each indicator it prints the three probabilities of the pivot against every other algorithm,
and writes ``Bayesian_<indicator>.png``: for each comparison, the samples of the posterior
distribution on a triangle whose corners are the three outcomes. The closer the cloud of points is
to a corner, the more probable that outcome.

The test is SAES's ``bayesian_sign_test`` (https://github.com/jMetal/SAES), which must be installed
(``pip install SAES``).

Example:
    python scripts/bayesian_plots.py \\
        results/tutorial-e9/validation/QualityIndicatorSummary.csv \\
        --pivot NSGAIIWFG --indicators HV --rope 0.001 --output-dir results/tutorial-e9/tables
"""

import argparse
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402

from study_summary import indicators_of, is_maximized, load_summary, median_table  # noqa: E402

SEED = 1
SAMPLE_SIZE = 5000
# Corners of the triangle: the pivot is better, the pivot is worse, practically equivalent
CORNERS = np.array([[0.0, 0.0], [1.0, 0.0], [0.5, np.sqrt(3) / 2]])


def to_triangle(samples: np.ndarray) -> np.ndarray:
    """Barycentric coordinates of samples (p_better, p_equivalent, p_worse) in the triangle."""
    better, equivalent, worse = samples[:, 0], samples[:, 1], samples[:, 2]
    weights = np.column_stack([better, worse, equivalent])
    return weights @ CORNERS


def plot_comparison(ax, samples: np.ndarray, pivot: str, other: str, probabilities) -> None:
    points = to_triangle(samples)
    ax.plot(*np.vstack([CORNERS, CORNERS[:1]]).T, color="black", linewidth=1)
    ax.scatter(points[:, 0], points[:, 1], s=2, alpha=0.3, color="tab:blue")
    better, equivalent, worse = probabilities
    labels = [(f"{pivot} better", better), (f"{other} better", worse), ("equivalent", equivalent)]
    offsets = [(-0.02, -0.12, "left"), (1.02, -0.12, "right"), (0.5, 0.9, "center")]
    for (label, probability), (x, y, align) in zip(labels, offsets, strict=True):
        ax.text(x, y, f"{label}\n{probability:.3f}", ha=align, fontsize=8)
    ax.set_xlim(-0.1, 1.1)
    ax.set_ylim(-0.15, 1.0)
    ax.set_aspect("equal")
    ax.axis("off")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Bayesian sign test (SAES) of a jMetal QualityIndicatorSummary.csv"
    )
    parser.add_argument("summary", type=Path, help="jMetal's QualityIndicatorSummary.csv")
    parser.add_argument("--pivot", required=True, help="algorithm compared with the others")
    parser.add_argument(
        "--algorithms", help="comma-separated algorithms to include (default: all in the file)"
    )
    parser.add_argument(
        "--indicators", help="comma-separated indicators (default: all in the file)"
    )
    parser.add_argument(
        "--rope", type=float, default=0.001, help="half-width of the region of practical equivalence"
    )
    parser.add_argument("--output-dir", type=Path, default=Path("."))
    args = parser.parse_args()

    try:
        from SAES.statistical_tests.bayesian import bayesian_sign_test
    except ImportError:
        sys.exit("SAES is not installed: pip install SAES")

    algorithms = args.algorithms.split(",") if args.algorithms else None
    data = load_summary(args.summary, algorithms)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for indicator in indicators_of(data, args.indicators):
        medians = median_table(data, indicator)
        others = [column for column in medians.columns if column != args.pivot]
        # The test sees a lower value as a win of the first column: negate maximized indicators
        sign = -1.0 if is_maximized(indicator) else 1.0
        figure, axes = plt.subplots(1, len(others), figsize=(4.5 * len(others), 4.2), squeeze=False)
        print(f"{indicator} (ROPE {args.rope}): P({args.pivot} better), P(equivalent), P(worse)")
        for ax, other in zip(axes[0], others, strict=True):
            probabilities, samples = bayesian_sign_test(
                sign * medians[[args.pivot, other]].values,
                rope_limits=[-args.rope, args.rope],
                sample_size=SAMPLE_SIZE,
                seed=SEED,
            )
            better, equivalent, worse = probabilities
            print(f"  against {other}: {better:.3f}, {equivalent:.3f}, {worse:.3f}")
            plot_comparison(ax, samples, args.pivot, other, probabilities)
        figure.suptitle(f"Bayesian sign test, {indicator} (ROPE = {args.rope})")
        figure.tight_layout()
        png_file = args.output_dir / f"Bayesian_{indicator}.png"
        figure.savefig(png_file, dpi=150)
        plt.close(figure)
        print(f"Written {png_file}\n")


if __name__ == "__main__":
    main()
