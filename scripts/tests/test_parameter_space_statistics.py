"""Tests of the size of a parameter space (plot_parameter_space.py --stats).

Run from the root of the repository: ``python -m pytest scripts/tests``.
"""

import sys
from pathlib import Path

import pytest
import yaml

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from plot_parameter_space import space_statistics  # noqa: E402

PARAMETER_SPACES = Path(__file__).resolve().parents[2] / "src/main/resources/parameterSpaces"

SMALL_SPACE = """
algorithmResult:
  type: categorical
  values:
    population: {}
    externalArchive:
      conditionalParameters:
        populationSizeWithArchive:
          type: integer
          range: [10, 200]
        archiveType:
          type: categorical
          values: [crowdingDistanceArchive, unboundedArchive]
crossover:
  type: categorical
  globalSubParameters:
    crossoverProbability:
      type: double
      range: [0.0, 1.0]
    crossoverRepairStrategy:
      type: categorical
      values: [random, round, bounds]
  values:
    SBX:
      conditionalParameters:
        sbxDistributionIndex:
          type: double
          range: [5.0, 400.0]
    wholeArithmetic:
"""


def statistics(text: str) -> dict[str, int]:
    values = space_statistics(yaml.safe_load(text))
    return {label.split(" ")[0]: value for label, value in values.items()}


class TestSpaceStatistics:
    def test_given_a_space_then_every_parameter_is_a_gene(self):
        assert statistics(SMALL_SPACE)["parameters"] == 7

    def test_given_a_space_then_the_structures_combine_the_categorical_values(self):
        # algorithmResult: population (1) + externalArchive (2 archive types) = 3;
        # crossover: (SBX + wholeArithmetic) x 3 repair strategies = 6
        assert statistics(SMALL_SPACE)["structures"] == 18

    def test_given_a_space_then_the_depth_counts_the_levels_below_the_top(self):
        assert statistics(SMALL_SPACE)["depth"] == 1
        assert statistics(SMALL_SPACE)["top-level"] == 2

    @pytest.mark.parametrize(
        ("file", "genes"),
        [("NSGAIIDouble.yaml", 34), ("NSGAIIDoubleReduced.yaml", 20), ("NSGAIIDoubleGECCO2019.yaml", 18)],
    )
    def test_given_a_bundled_space_then_the_genes_match_the_flat_encoding(self, file, genes):
        # The number of variables of MetaOptimizationProblem for these spaces
        data = yaml.safe_load((PARAMETER_SPACES / file).read_text())

        assert statistics(yaml.safe_dump(data))["parameters"] == genes
