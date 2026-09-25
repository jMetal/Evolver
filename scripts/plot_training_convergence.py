"""Plot how each meta-objective of a training run converges over the meta-evaluations.

For every indicator (meta-objective) in the training's INDICATORS.csv, draws a line with the median
value, at each checkpoint, of the configurations on the meta-optimizer's front, and a band between
the best and the worst value. With several training runs (replications of the same experiment),
the configurations of all of them are pooled at each checkpoint. For the primary indicator, a
dashed line marks the meta-evaluation at which 95% of the total improvement is reached (the median
over runs), a simple estimate of when the training stops improving.

Each argument is a training output directory (with INDICATORS.csv, as written by Evolver's
ConsolidatedOutputResults), or a directory holding one such directory per replication:

    python scripts/plot_training_convergence.py results/tutorial/E3 --primary NHV
    python scripts/plot_training_convergence.py path/to/re3d_15x5 --primary IGD+ --output-dir plots

HVMinus (the minimized form of the hypervolume) is displayed as HV.
"""

import argparse
import re
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402

NON_INDICATOR_COLUMNS = ("Evaluation", "SolutionId", "run")


def training_directories(paths: list[Path]) -> list[Path]:
    """Expand each path into the training output directories it contains."""
    directories = []
    for path in paths:
        if (path / "INDICATORS.csv").exists():
            directories.append(path)
        else:
            directories.extend(
                sorted(p for p in path.iterdir() if p.is_dir() and (p / "INDICATORS.csv").exists())
            )
    if not directories:
        raise SystemExit(f"No INDICATORS.csv found in {', '.join(map(str, paths))}")
    return directories


def load_indicators(directories: list[Path]) -> pd.DataFrame:
    """All rows of the runs' INDICATORS.csv files, tagged with their run, without duplicates."""
    frames = []
    for directory in directories:
        frame = pd.read_csv(directory / "INDICATORS.csv")
        frame["run"] = str(directory)
        frames.append(frame)
    return pd.concat(frames, ignore_index=True).drop_duplicates()


def spread_by_checkpoint(indicators: pd.DataFrame, indicator: str) -> pd.DataFrame:
    """Median, best (min) and worst (max) value of an indicator at each checkpoint."""
    return (
        indicators.groupby("Evaluation")[indicator]
        .agg(median="median", best="min", worst="max")
        .reset_index()
        .sort_values("Evaluation")
    )


def improvement_checkpoint(run_indicators: pd.DataFrame, indicator: str) -> int:
    """First checkpoint at which a run reaches 95% of its total best-so-far improvement."""
    best_so_far = run_indicators.groupby("Evaluation")[indicator].min().sort_index().cummin()
    threshold = best_so_far.iloc[0] - 0.95 * (best_so_far.iloc[0] - best_so_far.iloc[-1])
    return int(best_so_far[best_so_far <= threshold].index[0])


def plot_indicator(
    spread: pd.DataFrame, indicator: str, label: str, runs: int, marker: float | None, output: Path
) -> None:
    """Draw the median line, the best-worst band and, optionally, the 95% improvement marker."""
    name, sign = ("HV", -1) if indicator == "HVMinus" else (indicator, 1)
    lower, upper = (spread["worst"], spread["best"]) if sign < 0 else (spread["best"], spread["worst"])
    plt.figure(figsize=(6, 4))
    plt.plot(spread["Evaluation"], sign * spread["median"], color="C0", label="median")
    plt.fill_between(
        spread["Evaluation"], sign * lower, sign * upper, color="C0", alpha=0.25,
        label="best-worst range",
    )
    if marker is not None:
        plt.axvline(marker, color="gray", linestyle="--", linewidth=1.2)
        x_min, x_max = spread["Evaluation"].min(), spread["Evaluation"].max()
        offset = 0.02 * (x_max - x_min)
        x, alignment = (
            (marker - offset, "right") if marker + offset > x_min + 0.92 * (x_max - x_min)
            else (marker + offset, "left")
        )
        y_min, y_max = plt.ylim()
        y = y_min + 0.06 * (y_max - y_min) if sign < 0 else y_max - 0.06 * (y_max - y_min)
        plt.text(x, y, f"{marker:.0f}", rotation=90, color="gray", fontsize=8, ha=alignment,
                 va="bottom" if sign < 0 else "top")
    plt.xlabel("Meta-evaluations")
    plt.ylabel(f"{name} (training set)")
    plt.title(f"Convergence: {label}, {name} (n={runs} run(s))")
    plt.legend()
    plt.tight_layout()
    plt.savefig(output, dpi=150)
    plt.close()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("paths", type=Path, nargs="+", help="training output directories")
    parser.add_argument("--primary", default=None,
                        help="primary indicator column (default: the last one in INDICATORS.csv)")
    parser.add_argument("--output-dir", type=Path, default=None,
                        help="where to write the plots (default: the first directory given)")
    parser.add_argument("--label", default=None, help="name of the experiment in the titles")
    arguments = parser.parse_args()

    directories = training_directories(arguments.paths)
    indicators = load_indicators(directories)
    names = [c for c in indicators.columns if c not in NON_INDICATOR_COLUMNS]
    primary = arguments.primary or names[-1]
    if primary not in names:
        raise SystemExit(f"Unknown indicator {primary}; available: {', '.join(names)}")
    output_dir = arguments.output_dir or arguments.paths[0]
    output_dir.mkdir(parents=True, exist_ok=True)
    label = arguments.label or arguments.paths[0].resolve().name

    checkpoints = [improvement_checkpoint(run, primary) for _, run in indicators.groupby("run")]
    marker = float(pd.Series(checkpoints).median())
    for indicator in names:
        output = output_dir / f"convergence_{re.sub(r'[^A-Za-z0-9]+', '', indicator)}.png"
        plot_indicator(spread_by_checkpoint(indicators, indicator), indicator, label,
                       len(directories), marker if indicator == primary else None, output)
        print(f"Saved {output}")

    last = indicators[indicators["Evaluation"] == indicators["Evaluation"].max()]
    finals = last.groupby("run")[primary].min()
    print(f"{len(directories)} run(s); final best {primary}: median {finals.median():.6g}, "
          f"range {finals.min():.6g}-{finals.max():.6g}; 95% of the improvement reached at "
          f"{marker:.0f} meta-evaluations (median; range {min(checkpoints)}-{max(checkpoints)})")


if __name__ == "__main__":
    main()
