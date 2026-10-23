package org.uma.evolver.util.constrainthandling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.bounds.Bounds;

@DisplayName("Unit tests for class ConstrainedBoundedArchives")
class ConstrainedBoundedArchivesTest {

  private static final int SIZE = 10;

  static Stream<Arguments> archives() {
    return Stream.of(
        archive("crowdingDistance", () -> ConstrainedBoundedArchives.crowdingDistance(SIZE)),
        archive("angle", () -> ConstrainedBoundedArchives.angle(SIZE, true)),
        archive(
            "spatialSpreadDeviation",
            () -> ConstrainedBoundedArchives.spatialSpreadDeviation(SIZE)),
        archive("knnDistance", () -> ConstrainedBoundedArchives.knnDistance(SIZE, 2)),
        archive("hypervolume", () -> ConstrainedBoundedArchives.hypervolume(SIZE)));
  }

  private static Arguments archive(
      String name, Supplier<BoundedArchive<DoubleSolution>> archive) {
    return Arguments.of(name, archive);
  }

  private static DoubleSolution solution(double f1, double f2, double constraint) {
    var solution =
        new DefaultDoubleSolution(List.of(Bounds.create(0.0, 1.0)), 2, 1);
    solution.objectives()[0] = f1;
    solution.objectives()[1] = f2;
    solution.constraints()[0] = constraint;
    return solution;
  }

  @Nested
  @DisplayName("When a solution is infeasible")
  class InfeasibleSolution {

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.uma.evolver.util.constrainthandling.ConstrainedBoundedArchivesTest#archives")
    @DisplayName("an infeasible solution with better objectives does not remove a feasible one")
    void givenAFeasibleSolution_whenABetterInfeasibleOneIsAdded_thenTheFeasibleOneStays(
        String name, Supplier<BoundedArchive<DoubleSolution>> archiveSupplier) {
      // Arrange
      BoundedArchive<DoubleSolution> archive = archiveSupplier.get();
      DoubleSolution feasible = solution(1.0, 1.0, 0.0);
      archive.add(feasible);

      // Act
      archive.add(solution(0.0, 0.0, -1.0));

      // Assert
      assertEquals(1, archive.size());
      assertSame(feasible, archive.get(0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.uma.evolver.util.constrainthandling.ConstrainedBoundedArchivesTest#archives")
    @DisplayName("a feasible solution removes an infeasible one with better objectives")
    void givenAnInfeasibleSolution_whenAFeasibleOneIsAdded_thenItReplacesIt(
        String name, Supplier<BoundedArchive<DoubleSolution>> archiveSupplier) {
      // Arrange
      BoundedArchive<DoubleSolution> archive = archiveSupplier.get();
      archive.add(solution(0.0, 0.0, -1.0));
      DoubleSolution feasible = solution(1.0, 1.0, 0.0);

      // Act
      archive.add(feasible);

      // Assert
      assertEquals(1, archive.size());
      assertSame(feasible, archive.get(0));
    }
  }

  @Nested
  @DisplayName("When every solution is feasible")
  class FeasibleSolutions {

    @ParameterizedTest(name = "{0}")
    @MethodSource("org.uma.evolver.util.constrainthandling.ConstrainedBoundedArchivesTest#archives")
    @DisplayName("it keeps the non-dominated solutions, as plain Pareto dominance does")
    void givenFeasibleSolutions_whenAdded_thenTheNonDominatedOnesAreKept(
        String name, Supplier<BoundedArchive<DoubleSolution>> archiveSupplier) {
      // Arrange
      BoundedArchive<DoubleSolution> archive = archiveSupplier.get();

      // Act
      archive.add(solution(0.0, 1.0, 0.0));
      archive.add(solution(1.0, 0.0, 0.0));
      archive.add(solution(1.0, 1.0, 0.0));

      // Assert
      assertEquals(2, archive.size());
    }
  }
}
