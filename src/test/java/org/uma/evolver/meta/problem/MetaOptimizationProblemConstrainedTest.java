package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConstrainedMultiobjectiveIndicator;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP2;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

@DisplayName("Unit tests for the meta-optimization problem on problems with constraints")
class MetaOptimizationProblemConstrainedTest {

  private static String[] configuration(String file) throws IOException {
    return Files.readString(Path.of("src/main/resources/defaultConfigurations", file))
        .trim()
        .split("\\s+");
  }

  private static MetaOptimizationProblem<DoubleSolution> metaProblem(
      String space, Problem<DoubleSolution> problem, String front, int evaluations) {
    return new MetaOptimizationProblem<>(
        new DoubleNSGAII(100, new YAMLParameterSpace(space, new DoubleParameterFactory())),
        List.of(problem),
        List.of(front),
        List.<QualityIndicator>of(new Epsilon(), new NormalizedHypervolume()),
        new FixedEvaluationsStrategy(List.of(evaluations)),
        2);
  }

  @Test
  @DisplayName("a configuration that finds feasible solutions has values up to the thresholds")
  void givenAFeasibleConfiguration_whenEvaluated_thenItsValuesAreAtMostTheThresholds()
      throws IOException {
    // Arrange
    JMetalRandom.getInstance().setSeed(5);
    var meta =
        metaProblem(
            "NSGAIIDoubleConstrained.yaml",
            new Srinivas(),
            "resources/referenceFronts/Srinivas.csv",
            5000);

    // Act
    double[] values =
        meta.evaluateConfiguration(configuration("NSGAIIDoubleConstrainedDefault.txt"));

    // Assert
    assertTrue(values[0] <= 1.0, "EP " + values[0]);
    assertTrue(values[1] <= 1.0, "NHV " + values[1]);
  }

  @Test
  @DisplayName("a configuration without feasible solutions is above the failed-run weight")
  void givenAnInfeasibleConfiguration_whenEvaluated_thenItsValuesCountTheFailedRuns()
      throws IOException {
    // Arrange: NSGA-II without a constraint-handling technique ignores the constraints, and does
    // not reach the small feasible region of LIR-CMOP2
    JMetalRandom.getInstance().setSeed(5);
    var meta =
        metaProblem(
            "NSGAIIDouble.yaml", new LIRCMOP2(), "resources/referenceFronts/LIRCMOP2.csv", 5000);

    // Act
    double[] values = meta.evaluateConfiguration(configuration("NSGAIIDoubleDefault.txt"));

    // Assert: two failed runs (12), their violation (below 2) and the thresholds (1)
    for (double value : values) {
      assertTrue(value >= 12.0 + 1.0 && value < 12.0 + 2.0 + 1.0, "value " + value);
    }
  }

  @Test
  @DisplayName("ICMOP of a configuration that finds feasible solutions is in [-1, 1]")
  void givenIcmop_whenAFeasibleConfigurationIsEvaluated_thenItsValueIsAtMostTheThreshold()
      throws IOException {
    // Arrange
    JMetalRandom.getInstance().setSeed(5);
    var meta =
        new MetaOptimizationProblem<>(
            new DoubleNSGAII(
                100,
                new YAMLParameterSpace(
                    "NSGAIIDoubleConstrained.yaml", new DoubleParameterFactory())),
            List.<Problem<DoubleSolution>>of(new Srinivas()),
            List.of("resources/referenceFronts/Srinivas.csv"),
            List.<QualityIndicator>of(new ConstrainedMultiobjectiveIndicator()),
            new FixedEvaluationsStrategy(List.of(5000)),
            2);

    // Act
    double value =
        meta.evaluateConfiguration(configuration("NSGAIIDoubleConstrainedDefault.txt"))[0];

    // Assert: minus a hypervolume close to that of the reference front
    assertTrue(value >= -1.0 && value <= 1.0, "ICMOP " + value);
    assertTrue(value < 0.0, "ICMOP " + value);
  }

  @Test
  @DisplayName("an indicator without a threshold is rejected on a problem with constraints")
  void givenAnIndicatorWithoutThreshold_whenBuilt_thenItFails() {
    // Act and Assert
    assertThrows(
        JMetalException.class,
        () ->
            new MetaOptimizationProblem<>(
                new DoubleNSGAII(
                    100,
                    new YAMLParameterSpace(
                        "NSGAIIDoubleConstrained.yaml", new DoubleParameterFactory())),
                List.<Problem<DoubleSolution>>of(new Srinivas()),
                List.of("resources/referenceFronts/Srinivas.csv"),
                List.<QualityIndicator>of(new GenerationalDistance()),
                new FixedEvaluationsStrategy(List.of(1000)),
                1));
  }
}
