package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.evolver.cli.IndicatorRegistry;
import org.uma.jmetal.qualityindicator.QualityIndicator;

/**
 * The meta-optimization problems build the indicators of each training problem once and share them
 * among the threads that evaluate configurations at once, so every indicator that Evolver offers
 * must give the same values when computed from several threads on the same instance.
 */
@DisplayName("Unit tests for the indicators shared by the evaluations of a meta-optimization problem")
class SharedIndicatorsTest {

  private static final int THREADS = 8;
  private static final int FRONTS = 40;

  static List<String> indicatorNames() {
    return IndicatorRegistry.registeredNames().stream().sorted().toList();
  }

  /** A front of random bi-objective points in [0, 1.2]^2, with the number of points given. */
  private static double[][] front(Random random, int points) {
    double[][] front = new double[points][2];
    for (double[] point : front) {
      point[0] = 1.2 * random.nextDouble();
      point[1] = 1.2 * random.nextDouble();
    }
    return front;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("indicatorNames")
  @DisplayName(
      "Given an indicator with a reference front, when computed from several threads at once,"
          + " then it gives the values of a sequential computation")
  void givenASharedIndicator_whenComputedFromSeveralThreads_thenTheValuesAreTheSequentialOnes(
      String name) throws Exception {
    // Arrange
    Random random = new Random(1);
    double[][] referenceFront = new double[100][];
    for (int i = 0; i < referenceFront.length; i++) {
      double x = i / 99.0;
      referenceFront[i] = new double[] {x, 1.0 - Math.sqrt(x)};
    }
    List<double[][]> fronts = new ArrayList<>();
    for (int i = 0; i < FRONTS; i++) {
      fronts.add(front(random, 20 + random.nextInt(80)));
    }
    QualityIndicator indicator = IndicatorRegistry.resolve(name);
    indicator.referenceFront(referenceFront);
    double[] expected = fronts.stream().mapToDouble(front -> indicator.compute(front)).toArray();

    // Act
    ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<double[]>> computations = new ArrayList<>();
    try {
      for (int t = 0; t < THREADS; t++) {
        computations.add(
            executor.submit(
                () -> {
                  start.await();
                  return fronts.stream().mapToDouble(front -> indicator.compute(front)).toArray();
                }));
      }
      start.countDown();

      // Assert
      for (Future<double[]> computation : computations) {
        double[] values = computation.get();
        for (int i = 0; i < FRONTS; i++) {
          assertEquals(expected[i], values[i], name + ", front " + i);
        }
      }
    } finally {
      executor.shutdownNow();
    }
  }
}
