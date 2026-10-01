"""Computing time of the checkpoints of a training run, for plots with time on the x axis.

Each checkpoint block of VAR_CONF.txt starts with ``# Evaluation: <n>`` and, since Evolver 2.2,
``# Time (min): <minutes>``, the computing time of the meta-optimizer at that checkpoint (written
whatever the stopping condition). INDICATORS.csv has no time, so the plots join both files on the
evaluation count. Older runs have no time lines: ``checkpoint_minutes`` then returns an empty
mapping and the callers fall back to, or stop with, the meta-evaluations.
"""

import re
from pathlib import Path

import numpy as np
import pandas as pd

_EVALUATION = re.compile(r"^# Evaluation: (\d+)\s*$")
_TIME = re.compile(r"^# Time \(min\): ([0-9.]+)\s*$")

#: Unit of the time axis: name, factor from minutes, and longest run (in minutes) it is used for.
TIME_UNITS = (("seconds", 60.0, 2.0), ("minutes", 1.0, 120.0), ("hours", 1.0 / 60.0, float("inf")))


def checkpoint_minutes(var_conf_file: Path) -> dict[int, float]:
    """Minutes of each checkpoint of a VAR_CONF.txt file, by evaluation count. A checkpoint written
    twice (the final one) keeps its last time; a missing file or old format gives ``{}``."""
    if not var_conf_file.exists():
        return {}
    times = {}
    evaluation = None
    for line in var_conf_file.read_text().splitlines():
        if match := _EVALUATION.match(line):
            evaluation = int(match.group(1))
        elif (match := _TIME.match(line)) and evaluation is not None:
            times[evaluation] = float(match.group(1))
            evaluation = None
    return times


def add_time(indicators: pd.DataFrame, directory: Path) -> pd.DataFrame:
    """The rows of one run's INDICATORS.csv with a ``Minutes`` column, taken from its
    VAR_CONF.txt; rows of a checkpoint without time get NaN."""
    times = checkpoint_minutes(directory / "VAR_CONF.txt")
    result = indicators.copy()
    result["Minutes"] = result["Evaluation"].map(times)
    return result


def time_unit(longest_minutes: float) -> tuple[str, float]:
    """The unit for a time axis whose longest run lasts ``longest_minutes``: seconds below 2
    minutes, minutes below 2 hours, hours otherwise. Returns its name and the factor that converts
    minutes to it."""
    for name, factor, limit in TIME_UNITS:
        if longest_minutes < limit:
            return name, factor
    return TIME_UNITS[-1][0], TIME_UNITS[-1][1]


def format_time(minutes: float, longest_minutes: float) -> str:
    """A checkpoint time in the unit chosen for ``longest_minutes``, e.g. ``3.2 min``."""
    name, factor = time_unit(longest_minutes)
    abbreviation = {"seconds": "s", "minutes": "min", "hours": "h"}[name]
    return f"{minutes * factor:.3g} {abbreviation}"


def common_grid(runs: dict[str, pd.DataFrame], points: int = 100) -> np.ndarray:
    """Common time grid (minutes) for several runs: ``points`` instants from the earliest first
    checkpoint to the latest last one."""
    first = min(run["Minutes"].min() for run in runs.values())
    last = max(run["Minutes"].max() for run in runs.values())
    return np.linspace(first, last, points)


def front_at(run: pd.DataFrame, minutes: float) -> pd.DataFrame:
    """The rows of the last checkpoint of a run written at or before ``minutes`` (empty before
    its first checkpoint): the front the run had at that instant, as a step function."""
    earlier = run[run["Minutes"] <= minutes]
    if earlier.empty:
        return earlier
    return earlier[earlier["Evaluation"] == earlier["Evaluation"].max()]
