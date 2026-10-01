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

With ``--x time`` the x axis is the computing time of the meta-optimizer instead, read from the
``# Time (min)`` lines of each run's VAR_CONF.txt (Evolver 2.2 or later; it is written whatever the
stopping condition). The unit is chosen from the longest run: seconds below 2 minutes, minutes
below 2 hours, hours otherwise. A single run is plotted at its own checkpoints; with several runs,
whose checkpoints fall at different instants, each run contributes at each of 100 common instants
the front of its last checkpoint up to then.

    python scripts/plot_training_convergence.py results/tutorial/E3 --primary NHV --x time
"""

import argparse
import re
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import pandas as pd  # noqa: E402

from training_time import add_time, common_grid, front_at, time_unit  # noqa: E402

NON_INDICATOR_COLUMNS = ("Evaluation", "SolutionId", "run", "Minutes")


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


def load_indicators(directories: list[Path], with_time: bool = False) -> pd.DataFrame:
    """All rows of the runs' INDICATORS.csv files, tagged with their run, without duplicates.
    With ``with_time``, also the ``Minutes`` of each checkpoint, from the run's VAR_CONF.txt."""
    frames = []
    for directory in directories:
        frame = pd.read_csv(directory / "INDICATORS.csv").drop_duplicates()
        if with_time:
            frame = add_time(frame, directory)
            if frame["Minutes"].isna().any():
                raise SystemExit(
                    f"{directory / 'VAR_CONF.txt'} has no '# Time (min)' line for every checkpoint"
                    " (runs before Evolver 2.2 do not record it): use --x evaluations"
                )
        frame["run"] = str(directory)
        frames.append(frame)
    return pd.concat(frames, ignore_index=True)


def spread_by_checkpoint(
    indicators: pd.DataFrame, indicator: str, x: str = "Evaluation"
) -> pd.DataFrame:
    """Median, best (min) and worst (max) value of an indicator at each checkpoint (``x`` is the
    column that identifies it: ``Evaluation``, or ``Minutes`` for a single run)."""
    return (
        indicators.groupby(x)[indicator]
        .agg(median="median", best="min", worst="max")
        .reset_index()
        .sort_values(x)
    )


def spread_over_time(indicators: pd.DataFrame, indicator: str) -> pd.DataFrame:
    """Median, best and worst value of an indicator at common instants (``Minutes``): one run is
    taken at its own checkpoints; several runs at 100 common instants, each with the front of its
    last checkpoint up to then."""
    runs = dict(tuple(indicators.groupby("run")))
    if len(runs) == 1:
        return spread_by_checkpoint(indicators, indicator, "Minutes")
    rows = []
    for minutes in common_grid(runs):
        values = pd.concat([front_at(run, minutes)[indicator] for run in runs.values()])
        if not values.empty:
            rows.append({"Minutes": minutes, "median": values.median(),
                         "best": values.min(), "worst": values.max()})
    return pd.DataFrame(rows)


def improvement_checkpoint(run_indicators: pd.DataFrame, indicator: str, x: str = "Evaluation"):
    """First checkpoint (its ``x`` value) at which a run reaches 95% of its total best-so-far
    improvement."""
    best_so_far = run_indicators.groupby(x)[indicator].min().sort_index().cummin()
    threshold = best_so_far.iloc[0] - 0.95 * (best_so_far.iloc[0] - best_so_far.iloc[-1])
    return best_so_far[best_so_far <= threshold].index[0]


def plot_indicator(
    spread: pd.DataFrame, indicator: str, label: str, runs: int, marker: float | None, output: Path,
    x: str = "Evaluation", x_label: str = "Meta-evaluations", x_factor: float = 1.0,
) -> None:
    """Draw the median line, the best-worst band and, optionally, the 95% improvement marker.
    ``x`` is the column of the x axis, shown as ``x_label`` after multiplying by ``x_factor``."""
    name, sign = ("HV", -1) if indicator == "HVMinus" else (indicator, 1)
    lower, upper = (spread["worst"], spread["best"]) if sign < 0 else (spread["best"], spread["worst"])
    xs = spread[x] * x_factor
    plt.figure(figsize=(6, 4))
    plt.plot(xs, sign * spread["median"], color="C0", label="median")
    plt.fill_between(
        xs, sign * lower, sign * upper, color="C0", alpha=0.25,
        label="best-worst range",
    )
    if marker is not None:
        marker = marker * x_factor
        plt.axvline(marker, color="gray", linestyle="--", linewidth=1.2)
        x_min, x_max = xs.min(), xs.max()
        offset = 0.02 * (x_max - x_min)
        x, alignment = (
            (marker - offset, "right") if marker + offset > x_min + 0.92 * (x_max - x_min)
            else (marker + offset, "left")
        )
        y_min, y_max = plt.ylim()
        y = y_min + 0.06 * (y_max - y_min) if sign < 0 else y_max - 0.06 * (y_max - y_min)
        plt.text(x, y, f"{marker:.3g}" if x_factor != 1.0 or marker % 1 else f"{marker:.0f}",
                 rotation=90, color="gray", fontsize=8, ha=alignment,
                 va="bottom" if sign < 0 else "top")
    plt.xlabel(x_label)
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
    parser.add_argument("--x", choices=("evaluations", "time"), default="evaluations",
                        help="x axis: meta-evaluations (default) or computing time")
    arguments = parser.parse_args()

    by_time = arguments.x == "time"
    directories = training_directories(arguments.paths)
    indicators = load_indicators(directories, with_time=by_time)
    names = [c for c in indicators.columns if c not in NON_INDICATOR_COLUMNS]
    primary = arguments.primary or names[-1]
    if primary not in names:
        raise SystemExit(f"Unknown indicator {primary}; available: {', '.join(names)}")
    output_dir = arguments.output_dir or arguments.paths[0]
    output_dir.mkdir(parents=True, exist_ok=True)
    label = arguments.label or arguments.paths[0].resolve().name

    x = "Minutes" if by_time else "Evaluation"
    if by_time:
        unit, factor = time_unit(indicators["Minutes"].max())
        x_label, suffix = f"Computing time ({unit})", "_time"
    else:
        unit, factor, x_label, suffix = "meta-evaluations", 1.0, "Meta-evaluations", ""
    checkpoints = [improvement_checkpoint(run, primary, x) for _, run in indicators.groupby("run")]
    marker = float(pd.Series(checkpoints).median())
    for indicator in names:
        output = output_dir / f"convergence_{re.sub(r'[^A-Za-z0-9]+', '', indicator)}{suffix}.png"
        spread = (spread_over_time(indicators, indicator) if by_time
                  else spread_by_checkpoint(indicators, indicator))
        plot_indicator(spread, indicator, label, len(directories),
                       marker if indicator == primary else None, output, x, x_label, factor)
        print(f"Saved {output}")

    finals = indicators.groupby("run").apply(
        lambda run: run[run["Evaluation"] == run["Evaluation"].max()][primary].min()
    )
    print(f"{len(directories)} run(s); final best {primary}: median {finals.median():.6g}, "
          f"range {finals.min():.6g}-{finals.max():.6g}; 95% of the improvement reached at "
          f"{marker * factor:.4g} {unit} (median; range "
          f"{min(checkpoints) * factor:.4g}-{max(checkpoints) * factor:.4g})")


if __name__ == "__main__":
    main()
