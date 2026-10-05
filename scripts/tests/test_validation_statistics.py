"""Tests of the statistics of the validation scripts (study_summary.py, effect_size_tables.py,
friedman_holm_tables.py).

Run from the root of the repository: ``python -m pytest scripts/tests``.
"""

import sys
from pathlib import Path

import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from effect_size_tables import a12, a12_table, magnitude  # noqa: E402
from friedman_holm_tables import average_ranks  # noqa: E402
from study_summary import load_summary, median_table  # noqa: E402


def summary(rows: list[tuple[str, str, str, float]]) -> pd.DataFrame:
    """A QualityIndicatorSummary with the given (algorithm, problem, indicator, value) rows, each
    one a run (its ExecutionId is its position among the runs of the same group)."""
    data = pd.DataFrame(rows, columns=["Algorithm", "Problem", "IndicatorName", "IndicatorValue"])
    data["ExecutionId"] = data.groupby(["Algorithm", "Problem", "IndicatorName"]).cumcount()
    return data[["Algorithm", "Problem", "IndicatorName", "ExecutionId", "IndicatorValue"]]


class TestA12:
    def test_given_a_pivot_always_better_when_minimized_then_a12_is_one(self):
        assert a12([0.1, 0.2], [0.3, 0.4], maximize=False) == 1.0

    def test_given_a_pivot_always_better_when_maximized_then_a12_is_one(self):
        assert a12([0.3, 0.4], [0.1, 0.2], maximize=True) == 1.0

    def test_given_identical_samples_then_a12_is_one_half(self):
        assert a12([0.1, 0.2, 0.3], [0.1, 0.2, 0.3], maximize=False) == 0.5

    def test_given_overlapping_samples_then_ties_count_one_half(self):
        # pivot 1 vs (1, 2): tie, better -> 1.5; pivot 3 vs (1, 2): worse, worse -> 0
        assert a12([1.0, 3.0], [1.0, 2.0], maximize=False) == pytest.approx(1.5 / 4)

    @pytest.mark.parametrize(
        ("value", "expected"),
        [
            (0.5, "negligible"),
            (0.55, "negligible"),
            (0.56, "small"),
            (0.64, "medium"),
            (0.71, "large"),
            (0.36, "medium"),
            (0.0, "large"),
        ],
    )
    def test_given_a_value_then_its_magnitude_follows_vargha_delaney(self, value, expected):
        assert magnitude(value) == expected

    def test_given_a_summary_then_the_table_compares_the_pivot_with_every_other_algorithm(self):
        data = summary(
            [
                ("A", "P1", "IGD+", 0.1),
                ("A", "P1", "IGD+", 0.2),
                ("B", "P1", "IGD+", 0.3),
                ("B", "P1", "IGD+", 0.4),
                ("Tuned", "P1", "IGD+", 0.3),
                ("Tuned", "P1", "IGD+", 0.4),
            ]
        )

        table = a12_table(data, "IGD+", "Tuned")

        assert list(table.columns) == ["A", "B"]
        assert table.loc["P1", "A"] == 0.0
        assert table.loc["P1", "B"] == 0.5


class TestMediansAndRanks:
    def test_given_runs_then_the_median_table_keeps_the_order_of_the_file(self):
        data = summary(
            [
                ("B", "P2", "HV", 0.5),
                ("B", "P1", "HV", 0.4),
                ("A", "P2", "HV", 0.1),
                ("A", "P2", "HV", 0.3),
                ("A", "P1", "HV", 0.2),
            ]
        )

        medians = median_table(data, "HV")

        assert list(medians.columns) == ["B", "A"]
        assert list(medians.index) == ["P2", "P1"]
        assert medians.loc["P2", "A"] == pytest.approx(0.2)

    def test_given_a_maximized_indicator_then_the_highest_median_gets_rank_one(self):
        medians = pd.DataFrame({"A": [0.9, 0.8], "B": [0.1, 0.9]}, index=["P1", "P2"])

        ranks = average_ranks(medians, maximize=True)

        assert ranks["A"] == pytest.approx(1.5)
        assert ranks["B"] == pytest.approx(1.5)

    def test_given_ties_then_they_share_their_rank(self):
        medians = pd.DataFrame({"A": [0.1], "B": [0.1], "C": [0.2]}, index=["P1"])

        ranks = average_ranks(medians, maximize=False)

        assert list(ranks) == [1.5, 1.5, 3.0]

    def test_given_a_list_of_algorithms_then_the_summary_keeps_them_in_that_order(
        self, tmp_path
    ):
        file = tmp_path / "QualityIndicatorSummary.csv"
        summary([("A", "P1", "HV", 0.1), ("B", "P1", "HV", 0.2), ("C", "P1", "HV", 0.3)]).to_csv(
            file, index=False
        )

        data = load_summary(file, ["C", "A"])

        assert list(data["Algorithm"]) == ["C", "A"]
