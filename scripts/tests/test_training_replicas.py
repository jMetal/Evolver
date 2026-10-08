"""Tests of the summary of the replications of a training (training_replicas.py).

Run from the root of the repository: ``python -m pytest scripts/tests``.
"""

import subprocess
import sys
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import training_replicas  # noqa: E402

SCRIPT = Path(__file__).resolve().parents[1] / "training_replicas.py"


def write_replication(directory: Path, checkpoints: list[tuple[int, list[tuple[float, float]]]]):
    """A training output directory with a VAR_CONF.txt: for each checkpoint, its evaluation and
    the (EP, NHV) of each configuration of its front; configuration i is ``--id <EP>``."""
    directory.mkdir(parents=True)
    lines = []
    for evaluation, front in checkpoints:
        lines += [f"# Evaluation: {evaluation}", f"# Time (min): {evaluation / 100:.3f}"]
        lines += [f"EP={ep} NHV={nhv} | --id {ep}" for ep, nhv in front]
        lines.append("")
    (directory / "VAR_CONF.txt").write_text("\n".join(lines))


@pytest.fixture
def study(tmp_path: Path) -> Path:
    """A study with two replications; the final checkpoint of run01 is written twice."""
    write_replication(tmp_path / "flat" / "run01",
                      [(100, [(0.5, 0.5)]), (200, [(0.1, 0.4), (0.3, 0.2)]),
                       (200, [(0.1, 0.4), (0.3, 0.2)])])
    write_replication(tmp_path / "flat" / "run02", [(200, [(0.2, 0.3)])])
    return tmp_path / "flat"


def test_last_front_is_the_last_checkpoint(study: Path):
    # Arrange & Act
    evaluation, front = training_replicas.last_front(study / "run01" / "VAR_CONF.txt")

    # Assert
    assert evaluation == 200
    assert [values for values, _ in front] == [{"EP": 0.1, "NHV": 0.4}, {"EP": 0.3, "NHV": 0.2}]


def test_summarize_keeps_the_best_values_and_the_configuration_of_the_primary(study: Path):
    # Arrange & Act
    table, chosen = training_replicas.summarize(study, "flat", "NHV")

    # Assert
    run01 = table[table["Replication"] == "run01"].iloc[0]
    assert (run01["EP"], run01["NHV"], run01["FrontSize"], run01["Minutes"]) == (0.1, 0.2, 2, 2.0)
    assert chosen == {"run01": "--id 0.3", "run02": "--id 0.2"}


def test_unknown_primary_stops(study: Path):
    # Arrange & Act & Assert
    with pytest.raises(SystemExit):
        training_replicas.summarize(study, "flat", "IGD+")


def test_two_studies_give_a_quality_indicator_summary(study: Path, tmp_path: Path):
    # Arrange
    write_replication(tmp_path / "tree" / "run01", [(200, [(0.05, 0.1)])])
    output = tmp_path / "analysis"

    # Act
    subprocess.run([sys.executable, str(SCRIPT), str(study), str(tmp_path / "tree"),
                    "--primary", "NHV", "--output-dir", str(output)], check=True,
                   capture_output=True)

    # Assert
    summary = pd.read_csv(output / "QualityIndicatorSummary.csv")
    assert list(summary.columns) == ["Algorithm", "Problem", "IndicatorName", "ExecutionId",
                                     "IndicatorValue"]
    assert sorted(summary["Algorithm"].unique()) == ["flat", "tree"]
    assert summary[(summary.Algorithm == "flat") & (summary.IndicatorName == "NHV")][
        "ExecutionId"].tolist() == [0, 1]
    assert (output / "configurations" / "tree" / "run01.txt").read_text() == "--id 0.05\n"
