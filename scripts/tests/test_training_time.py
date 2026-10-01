"""Tests of the time axis of the training plots (training_time.py, plot_training_convergence.py).

Run from the root of the repository: ``python -m pytest scripts/tests``.
"""

import sys
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import plot_training_convergence as convergence  # noqa: E402
from training_time import (  # noqa: E402
    checkpoint_minutes,
    format_time,
    front_at,
    time_unit,
)


def write_run(directory: Path, checkpoints: list[tuple[int, float | None, list[float]]]) -> Path:
    """A training output directory with INDICATORS.csv and VAR_CONF.txt: for each checkpoint, its
    evaluation, its minutes (None: an old run without time) and the NHV of its front."""
    directory.mkdir(parents=True)
    rows, var_conf = [], []
    for evaluation, minutes, values in checkpoints:
        var_conf.append(f"# Evaluation: {evaluation}")
        if minutes is not None:
            var_conf.append(f"# Time (min): {minutes:.3f}")
        for solution_id, value in enumerate(values):
            rows.append({"Evaluation": evaluation, "SolutionId": solution_id, "EP": 0.1,
                         "NHV": value})
            var_conf.append(f"EP=0.1 NHV={value} | --algorithmResult population")
        var_conf.append("")
    pd.DataFrame(rows).to_csv(directory / "INDICATORS.csv", index=False)
    (directory / "VAR_CONF.txt").write_text("\n".join(var_conf) + "\n")
    return directory


def test_given_var_conf_with_time_when_read_then_each_checkpoint_has_its_minutes(tmp_path):
    # Arrange
    run = write_run(tmp_path / "run", [(100, 0.5, [0.4]), (200, 1.25, [0.3]), (200, 1.3, [0.3])])

    # Act
    times = checkpoint_minutes(run / "VAR_CONF.txt")

    # Assert: the final checkpoint, written twice, keeps its last time
    assert times == {100: 0.5, 200: 1.3}


def test_given_var_conf_without_time_when_read_then_there_are_no_times(tmp_path):
    # Arrange
    run = write_run(tmp_path / "run", [(100, None, [0.4])])

    # Act / Assert
    assert checkpoint_minutes(run / "VAR_CONF.txt") == {}


@pytest.mark.parametrize(
    "longest, expected",
    [(0.5, ("seconds", 60.0)), (1.99, ("seconds", 60.0)), (2.0, ("minutes", 1.0)),
     (119.0, ("minutes", 1.0)), (120.0, ("hours", 1.0 / 60.0)), (900.0, ("hours", 1.0 / 60.0))],
)
def test_given_longest_run_when_choosing_unit_then_it_fits_the_duration(longest, expected):
    assert time_unit(longest) == expected


def test_given_minutes_when_formatting_then_the_unit_of_the_run_is_used():
    assert format_time(0.5, 1.0) == "30 s"
    assert format_time(3.2, 10.0) == "3.2 min"
    assert format_time(90.0, 300.0) == "1.5 h"


def test_given_a_run_when_asking_front_at_an_instant_then_its_last_checkpoint_is_returned(tmp_path):
    # Arrange
    run = pd.DataFrame({"Evaluation": [100, 200, 200], "Minutes": [1.0, 2.0, 2.0],
                        "NHV": [0.5, 0.3, 0.2]})

    # Act / Assert
    assert front_at(run, 0.5).empty
    assert list(front_at(run, 1.5)["NHV"]) == [0.5]
    assert list(front_at(run, 2.0)["NHV"]) == [0.3, 0.2]


def test_given_two_runs_when_spread_over_time_then_they_are_pooled_on_a_common_grid(tmp_path):
    # Arrange: run A improves at minute 2, run B at minute 3
    run_a = write_run(tmp_path / "a", [(100, 1.0, [0.5]), (200, 2.0, [0.2])])
    run_b = write_run(tmp_path / "b", [(100, 1.0, [0.6]), (200, 3.0, [0.1])])
    indicators = convergence.load_indicators([run_a, run_b], with_time=True)

    # Act
    spread = convergence.spread_over_time(indicators, "NHV")

    # Assert
    assert len(spread) == 100
    first, last = spread.iloc[0], spread.iloc[-1]
    assert (first["best"], first["worst"]) == (0.5, 0.6)
    assert (last["best"], last["worst"]) == (0.1, 0.2)
    at_2_5 = spread[(spread["Minutes"] > 2.4) & (spread["Minutes"] < 2.6)].iloc[0]
    assert (at_2_5["best"], at_2_5["worst"]) == (0.2, 0.6)


def test_given_a_run_without_time_when_loading_with_time_then_it_stops_with_a_message(tmp_path):
    # Arrange
    run = write_run(tmp_path / "old", [(100, None, [0.4])])

    # Act / Assert
    with pytest.raises(SystemExit, match="--x evaluations"):
        convergence.load_indicators([run], with_time=True)


def test_given_a_run_when_marking_the_improvement_by_time_then_the_minutes_are_returned(tmp_path):
    # Arrange
    run = write_run(tmp_path / "run", [(100, 1.0, [1.0]), (200, 2.0, [0.5]), (300, 4.0, [0.0])])
    indicators = convergence.load_indicators([run], with_time=True)

    # Act / Assert
    assert convergence.improvement_checkpoint(indicators, "NHV", "Minutes") == 4.0
    assert convergence.improvement_checkpoint(indicators, "NHV") == 300
