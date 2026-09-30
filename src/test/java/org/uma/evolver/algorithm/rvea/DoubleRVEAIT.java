package org.uma.evolver.algorithm.rvea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;

/**
 * Integration tests for {@link DoubleRVEA}: the three variants (RVEA, RVEA* and iRVEA) run end to
 * end with the standard configuration, and a meta-optimization problem evaluates a configuration
 * on a training set whose problems have two and three objectives, each with its own weight
 * vector file. The thresholds leave a wide margin: over 10 seeds, the IGD+ on DTLZ2 was between
 * 0.022 and 0.029 for every variant, and at most 0.068 on ZDT1.
 */
@DisplayName("Integration tests for class DoubleRVEA")
class DoubleRVEAIT {

  private static final int POPULATION_SIZE = 100;
  private static final String WEIGHT_VECTORS = "resources/weightVectors";
  private static final String CONFIGURATION =
      "--algorithmResult population --createInitialSolutions default "
          + "--variation crossoverAndMutationVariation --offspringPopulationSize 100 "
          + "--crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds "
          + "--sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 "
          + "--selection random --alpha 2.0 --fr 0.1 --numberOfSubregions 40 "
          + "--lateStageFraction 0.8 --epsilonKappa 0.05 --replacement ";

  private static double igdPlus(
      Problem<DoubleSolution> problem, String replacement, String referenceFront)
      throws IOException {
    var rvea =
        new DoubleRVEA(
            problem,
            POPULATION_SIZE,
            20000,
            WEIGHT_VECTORS,
            new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory()));
    rvea.parse((CONFIGURATION + replacement).split("\\s+"));
    var algorithm = rvea.build();
    algorithm.run();
    double[][] front = VectorUtils.readVectors("resources/referenceFronts/" + referenceFront, ",");
    return new InvertedGenerationalDistancePlus(front)
        .compute(SolutionListUtils.getMatrixWithObjectiveValues(algorithm.result()));
  }

  @Tag("integration")
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"rvea", "rveaStar", "iRVEA"})
  @DisplayName("given DTLZ2, when running each variant, then it approximates the front")
  void givenDtlz2_whenRunningEachVariant_thenItApproximatesTheFront(String replacement)
      throws IOException {
    // Act
    double igd = igdPlus(new DTLZ2(), replacement, "DTLZ2.3D.csv");

    // Assert
    assertTrue(igd < 0.05, replacement + ": IGD+ on DTLZ2 should be below 0.05 but was " + igd);
  }

  @Tag("integration")
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"rvea", "rveaStar", "iRVEA"})
  @DisplayName("given ZDT1, when running each variant, then it approximates the front")
  void givenZdt1_whenRunningEachVariant_thenItApproximatesTheFront(String replacement)
      throws IOException {
    // Act
    double igd = igdPlus(new ZDT1(), replacement, "ZDT1.csv");

    // Assert
    assertTrue(igd < 0.1, replacement + ": IGD+ on ZDT1 should be below 0.1 but was " + igd);
  }

  @Tag("integration")
  @Test
  @DisplayName(
      "given a training set with two and three objectives, when a configuration is evaluated,"
          + " then every problem gets its weight vectors")
  void givenTrainingSetWithTwoAndThreeObjectives_whenEvaluated_thenIndicatorsAreComputed() {
    // Arrange
    List<Problem<DoubleSolution>> problems = List.of(new ZDT1(), new DTLZ2());
    var metaProblem =
        new MetaOptimizationProblem<>(
            new DoubleRVEA(
                POPULATION_SIZE,
                WEIGHT_VECTORS,
                new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory())),
            problems,
            List.of("resources/referenceFronts/ZDT1.csv", "resources/referenceFronts/DTLZ2.3D.csv"),
            List.of(new Epsilon(), new NormalizedHypervolume()),
            new FixedEvaluationsStrategy(Collections.nCopies(problems.size(), 5000)),
            1);

    // Act
    double[] indicators = metaProblem.evaluateConfiguration((CONFIGURATION + "rvea").split("\\s+"));

    // Assert
    assertEquals(2, indicators.length);
    for (double value : indicators) {
      assertTrue(Double.isFinite(value), "Indicator values should be finite: " + value);
    }
  }
}
