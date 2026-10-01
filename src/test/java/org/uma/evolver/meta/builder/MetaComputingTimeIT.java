package org.uma.evolver.meta.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.algorithm.RandomSearch;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Integration tests of the meta-optimizers bounded by computing time: they run real
 * meta-optimizations (NSGA-II, SPEA2 and SMPSO on a meta-optimization problem with a cheap base
 * level, and random search) with limits of a fraction of a minute.
 */
@DisplayName("Meta-optimizers bounded by computing time")
@Tag("integration")
class MetaComputingTimeIT {

  private static final double LIMIT_MINUTES = 0.04; // 2.4 s
  private static final long LIMIT_MILLIS = 2400;
  private static final int POPULATION_SIZE = 10;
  /** Generous upper bound for the real time: the limit plus the generation in progress. */
  private static final long MAX_REAL_MILLIS = LIMIT_MILLIS + 30_000;

  private static MetaOptimizationProblem<DoubleSolution> metaProblem() {
    var baseAlgorithm =
        new DoubleNSGAII(
            20, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    List<Problem<DoubleSolution>> problems = List.of(new ZDT1());
    List<QualityIndicator> indicators = List.of(new NormalizedHypervolume(), new Epsilon());
    return new MetaOptimizationProblem<>(
        baseAlgorithm,
        problems,
        List.of("resources/referenceFronts/ZDT1.csv"),
        indicators,
        new FixedEvaluationsStrategy(List.of(400)),
        1);
  }

  @Test
  @DisplayName("Given a time limit, when running NSGA-II, then it stops after completing a generation")
  void givenTimeLimitWhenRunningNsgaIIThenItStopsAfterCompletingAGeneration() {
    // Arrange
    var algorithm =
        new MetaNSGAIIBuilder(
                metaProblem(),
                new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()))
            .setPopulationSize(POPULATION_SIZE)
            .setNumberOfCores(2)
            .setMaxComputingTimeMinutes(LIMIT_MINUTES)
            .build();

    // Act
    long start = System.currentTimeMillis();
    algorithm.run();
    long elapsed = System.currentTimeMillis() - start;

    // Assert
    assertTrue(elapsed >= LIMIT_MILLIS, "stopped before the limit: " + elapsed + " ms");
    assertTrue(elapsed < MAX_REAL_MILLIS, "overshoot too large: " + elapsed + " ms");
    assertTrue(algorithm.numberOfEvaluations() > POPULATION_SIZE, "no generation was run");
    assertEquals(
        0,
        algorithm.numberOfEvaluations() % POPULATION_SIZE,
        "the generation in progress must be completed");
    assertEquals(POPULATION_SIZE, algorithm.result().size());
  }

  @Test
  @DisplayName("Given a limit shorter than the initial population, when running, then no generation is run")
  void givenLimitShorterThanInitialPopulationWhenRunningThenNoGenerationIsRun() {
    // Arrange: 0.00001 minutes = 0.6 ms (rounded to 1 ms), less than the initial evaluation
    var algorithm =
        new MetaNSGAIIBuilder(
                metaProblem(),
                new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()))
            .setPopulationSize(POPULATION_SIZE)
            .setNumberOfCores(2)
            .setMaxComputingTimeMinutes(0.00001)
            .build();

    // Act
    algorithm.run();

    // Assert
    assertEquals(POPULATION_SIZE, algorithm.numberOfEvaluations());
    assertEquals(POPULATION_SIZE, algorithm.result().size());
  }

  @Test
  @DisplayName("Given a time limit, when running SPEA2, then it stops after completing a generation")
  void givenTimeLimitWhenRunningSpea2ThenItStopsAfterCompletingAGeneration() {
    // Arrange
    var algorithm =
        new MetaSPEA2Builder(metaProblem())
            .setPopulationSize(POPULATION_SIZE)
            .setNumberOfCores(2)
            .setMaxComputingTimeMinutes(LIMIT_MINUTES)
            .build();

    // Act
    long start = System.currentTimeMillis();
    algorithm.run();
    long elapsed = System.currentTimeMillis() - start;

    // Assert
    assertTrue(elapsed >= LIMIT_MILLIS, "stopped before the limit: " + elapsed + " ms");
    assertTrue(elapsed < MAX_REAL_MILLIS, "overshoot too large: " + elapsed + " ms");
    assertEquals(0, algorithm.numberOfEvaluations() % POPULATION_SIZE);
  }

  @Test
  @DisplayName("Given a time limit, when running SMPSO, then it stops after the limit")
  void givenTimeLimitWhenRunningSmpsoThenItStopsAfterTheLimit() {
    // Arrange
    var algorithm =
        new MetaSMPSOBuilder(metaProblem())
            .setSwarmSize(POPULATION_SIZE)
            .setNumberOfCores(2)
            .setMaxComputingTimeMinutes(LIMIT_MINUTES)
            .build();

    // Act
    long start = System.currentTimeMillis();
    algorithm.run();
    long elapsed = System.currentTimeMillis() - start;

    // Assert
    assertTrue(elapsed >= LIMIT_MILLIS, "stopped before the limit: " + elapsed + " ms");
    assertTrue(elapsed < MAX_REAL_MILLIS, "overshoot too large: " + elapsed + " ms");
    assertFalse(algorithm.result().isEmpty());
  }

  @Test
  @DisplayName("Given a time limit, when running the asynchronous NSGA-II, then it stops after the limit with a whole population")
  void givenTimeLimitWhenRunningAsyncNsgaIIThenItStopsAfterTheLimitWithAWholePopulation() {
    // Arrange
    var algorithm =
        new MetaAsyncNSGAIIBuilder(metaProblem())
            .setPopulationSize(POPULATION_SIZE)
            .setNumberOfCores(2)
            .setMaxComputingTimeMinutes(LIMIT_MINUTES)
            .build();

    // Act
    long start = System.currentTimeMillis();
    algorithm.run();
    long elapsed = System.currentTimeMillis() - start;

    // Assert
    assertTrue(elapsed >= LIMIT_MILLIS, "stopped before the limit: " + elapsed + " ms");
    assertTrue(elapsed < MAX_REAL_MILLIS, "overshoot too large: " + elapsed + " ms");
    assertEquals(POPULATION_SIZE, algorithm.result().size());
  }

  @Test
  @DisplayName("Given a limit shorter than the initial population, when running the asynchronous NSGA-II, then the initial population is evaluated")
  void givenLimitShorterThanInitialPopulationWhenRunningAsyncNsgaIIThenInitialPopulationIsEvaluated() {
    // Arrange
    var algorithm =
        new MetaAsyncNSGAIIBuilder(metaProblem())
            .setPopulationSize(POPULATION_SIZE)
            .setNumberOfCores(2)
            .setMaxComputingTimeMinutes(0.00001)
            .build();

    // Act
    algorithm.run();

    // Assert
    assertEquals(POPULATION_SIZE, algorithm.result().size());
  }

  @Test
  @DisplayName("Given a time limit, when running random search, then it evaluates whole batches")
  void givenTimeLimitWhenRunningRandomSearchThenItEvaluatesWholeBatches() {
    // Arrange
    int cores = 2;
    RandomSearch<DoubleSolution> randomSearch =
        new MetaRandomSearchBuilder<>(metaProblem())
            .setNumberOfCores(cores)
            .setMaxComputingTimeMinutes(LIMIT_MINUTES)
            .build();

    // Act
    randomSearch.run();

    // Assert
    assertTrue(randomSearch.totalComputingTime() >= LIMIT_MILLIS);
    assertTrue(randomSearch.totalComputingTime() < MAX_REAL_MILLIS);
    assertTrue(randomSearch.numberOfEvaluations() >= cores);
    assertEquals(0, randomSearch.numberOfEvaluations() % cores);
    assertFalse(randomSearch.result().isEmpty());
  }
}
