package org.uma.evolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.maf.MaF01;
import org.uma.jmetal.problem.multiobjective.maf.MaF02;
import org.uma.jmetal.problem.multiobjective.maf.MaF03;
import org.uma.jmetal.problem.multiobjective.maf.MaF04;
import org.uma.jmetal.problem.multiobjective.maf.MaF05;
import org.uma.jmetal.problem.multiobjective.maf.MaF06;
import org.uma.jmetal.problem.multiobjective.maf.MaF07;
import org.uma.jmetal.problem.multiobjective.maf.MaF09;
import org.uma.jmetal.problem.multiobjective.maf.MaF10;
import org.uma.jmetal.problem.multiobjective.maf.MaF11;
import org.uma.jmetal.problem.multiobjective.maf.MaF13;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * The reference fronts of the MaF problems with 3 objectives ({@code
 * resources/referenceFronts/MaFxx.3D.csv}, written by {@code scripts/maf_reference_fronts.py})
 * against the problems of jMetal: solutions sampled from the Pareto set of each problem are
 * evaluated: no sample may dominate a point of the front (the front is optimal), and every sample
 * that the front does not dominate must lie on it, close to one of its points (the front covers
 * what the problem can reach). The fronts of MaF07 and MaF11 are disconnected: the samples of the
 * Pareto set that fall between their parts are dominated by the front.
 *
 * <p>MaF12 (a sphere), MaF14 (the simplex) and MaF15 (an inverted sphere) are left out: their
 * Pareto sets are not easy to sample, and their fronts are closed formulas.
 */
@Tag("integration")
@DisplayName("Reference fronts of the MaF problems with 3 objectives")
class MaFReferenceFrontsIT {

  private static final int OBJECTIVES = 3;
  private static final int SAMPLES = 2000;
  private static final double TOLERANCE = 0.02;

  /** Sets the variables of a solution of the Pareto set: random position, optimal distance. */
  private static BiConsumer<DoubleSolution, Random> positionAndDistance(
      int positionVariables, double distanceValue) {
    return (solution, random) -> {
      for (int i = 0; i < solution.variables().size(); i++) {
        solution.variables().set(i, i < positionVariables ? random.nextDouble() : distanceValue);
      }
    };
  }

  /** WFG-like problems (MaF10, MaF11): x_i in [0, 2i], distance variables at 0.35 of the range. */
  private static BiConsumer<DoubleSolution, Random> wfg() {
    return (solution, random) -> {
      for (int i = 0; i < solution.variables().size(); i++) {
        double upper = 2.0 * (i + 1);
        solution.variables().set(i, i < OBJECTIVES - 1 ? random.nextDouble() * upper : 0.35 * upper);
      }
    };
  }

  /** MaF09: any point of the triangle (the polygon of 3 vertices). */
  private static BiConsumer<DoubleSolution, Random> insideTriangle() {
    double[][] vertices = MaF09.polygonpoints(OBJECTIVES, 1.0);
    return (solution, random) -> {
      double a = random.nextDouble();
      double b = random.nextDouble();
      if (a + b > 1) {
        a = 1 - a;
        b = 1 - b;
      }
      for (int j = 0; j < 2; j++) {
        double value =
            vertices[0][j] + a * (vertices[1][j] - vertices[0][j]) + b * (vertices[2][j] - vertices[0][j]);
        solution.variables().set(j, value);
      }
    };
  }

  /** MaF13: x_i = 2 x_2 sin(2 pi x_1 + i pi / n) for i >= 3. */
  private static BiConsumer<DoubleSolution, Random> maf13() {
    return (solution, random) -> {
      int n = solution.variables().size();
      double x1 = random.nextDouble();
      double x2 = random.nextDouble();
      solution.variables().set(0, x1);
      solution.variables().set(1, x2);
      for (int i = 2; i < n; i++) {
        solution.variables().set(i, 2 * x2 * Math.sin(2 * Math.PI * x1 + (i + 1) * Math.PI / n));
      }
    };
  }

  static Stream<Arguments> problems() {
    return Stream.of(
        Arguments.of("MaF01", new MaF01(12, OBJECTIVES), positionAndDistance(2, 0.5)),
        Arguments.of("MaF02", new MaF02(12, OBJECTIVES), positionAndDistance(2, 0.5)),
        Arguments.of("MaF03", new MaF03(12, OBJECTIVES), positionAndDistance(2, 0.5)),
        Arguments.of("MaF04", new MaF04(12, OBJECTIVES), positionAndDistance(2, 0.5)),
        Arguments.of("MaF05", new MaF05(12, OBJECTIVES), positionAndDistance(2, 0.5)),
        Arguments.of("MaF06", new MaF06(12, OBJECTIVES), positionAndDistance(2, 0.5)),
        Arguments.of("MaF07", new MaF07(22, OBJECTIVES), positionAndDistance(2, 0.0)),
        Arguments.of("MaF09", new MaF09(2, OBJECTIVES), insideTriangle()),
        Arguments.of("MaF10", new MaF10(12, OBJECTIVES), wfg()),
        Arguments.of("MaF11", new MaF11(12, OBJECTIVES), wfg()),
        Arguments.of("MaF13", new MaF13(5, OBJECTIVES), maf13()));
  }

  private static List<double[]> readFront(String name) throws IOException {
    List<double[]> front = new ArrayList<>();
    for (String line : Files.readAllLines(Path.of("resources/referenceFronts", name + ".3D.csv"))) {
      if (!line.isBlank()) {
        front.add(Arrays.stream(line.split(",")).mapToDouble(Double::parseDouble).toArray());
      }
    }
    return front;
  }

  private static boolean dominates(double[] a, double[] b, double margin) {
    boolean strictly = false;
    for (int j = 0; j < a.length; j++) {
      if (a[j] > b[j] - margin) {
        return false;
      }
      strictly = true;
    }
    return strictly;
  }

  /**
   * Whether {@code a} dominates {@code b} up to {@code margin}: not worse than {@code b} by more
   * than the margin in any objective, and better by more than it in one. A point of the front
   * dominates in this sense the weakly optimal solutions of the border of a front (MaF11 with an
   * objective near 0), which the front, sampled from 1e-6 on, does not contain.
   */
  private static boolean dominatesWithin(double[] a, double[] b, double margin) {
    boolean better = false;
    for (int j = 0; j < a.length; j++) {
      if (a[j] > b[j] + margin) {
        return false;
      }
      better |= a[j] < b[j] - margin;
    }
    return better;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("problems")
  @DisplayName(
      "given samples of the Pareto set, when evaluated by jMetal, then none dominates the reference"
          + " front and those it does not dominate lie on it")
  void givenParetoSetSamples_whenEvaluated_thenTheyLieOnTheFront(
      String name, DoubleProblem problem, BiConsumer<DoubleSolution, Random> paretoSet)
      throws IOException {
    // Arrange
    List<double[]> front = readFront(name);
    double scale = 0;
    for (int j = 0; j < OBJECTIVES; j++) {
      final int objective = j;
      double min = front.stream().mapToDouble(p -> p[objective]).min().orElseThrow();
      double max = front.stream().mapToDouble(p -> p[objective]).max().orElseThrow();
      scale = Math.max(scale, max - min);
    }
    Random random = new Random(1);
    List<double[]> samples = new ArrayList<>();

    // Act
    for (int s = 0; s < SAMPLES; s++) {
      DoubleSolution solution = problem.createSolution();
      paretoSet.accept(solution, random);
      problem.evaluate(solution);
      samples.add(solution.objectives().clone());
    }

    // Assert
    double margin = TOLERANCE * scale;
    for (double[] sample : samples) {
      for (double[] point : front) {
        assertFalse(
            dominates(sample, point, margin),
            name + ": " + Arrays.toString(sample) + " dominates " + Arrays.toString(point));
      }
    }
    for (double[] sample : samples) {
      boolean dominatedByTheFront =
          front.stream().anyMatch(point -> dominatesWithin(point, sample, margin));
      if (dominatedByTheFront) {
        continue;
      }
      double nearest = Double.POSITIVE_INFINITY;
      for (double[] point : front) {
        double distance = 0;
        for (int j = 0; j < OBJECTIVES; j++) {
          distance += (sample[j] - point[j]) * (sample[j] - point[j]);
        }
        nearest = Math.min(nearest, Math.sqrt(distance));
      }
      assertTrue(
          nearest <= margin,
          name + ": " + Arrays.toString(sample) + " is " + nearest + " from the front");
    }
  }
}
