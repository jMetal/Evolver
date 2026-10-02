package org.uma.evolver.cli.solving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observable.impl.DefaultObservable;

@DisplayName("Unit tests for class SolveFrontObserver")
class SolveFrontObserverTest {

  @TempDir private Path tempDir;

  private Path frontFile() {
    return tempDir.resolve(SolveFrontObserver.FILE_NAME);
  }

  /** A population of ZDT1 solutions with the given objective values (no evaluation needed). */
  private static List<DoubleSolution> population(double[]... objectives) {
    ZDT1 problem = new ZDT1();
    List<DoubleSolution> population = new ArrayList<>();
    for (double[] values : objectives) {
      DoubleSolution solution = problem.createSolution();
      solution.objectives()[0] = values[0];
      solution.objectives()[1] = values[1];
      population.add(solution);
    }
    return population;
  }

  private void report(
      SolveFrontObserver observer, int evaluations, List<DoubleSolution> population) {
    observer.update(
        new DefaultObservable<>("test"),
        Map.of("EVALUATIONS", evaluations, "POPULATION", population));
  }

  @Test
  @DisplayName("given fewer evaluations than the frequency, when reported, then nothing is written")
  void givenFewerEvaluationsThanTheFrequency_whenReported_thenNothingIsWritten() {
    // Arrange
    var observer = new SolveFrontObserver(frontFile(), 1, 500, false);

    // Act
    report(observer, 400, population(new double[] {0.5, 0.5}));

    // Assert
    assertFalse(Files.exists(frontFile()));
  }

  @Test
  @DisplayName("given the frequency reached, when reported, then the front is written")
  void givenTheFrequencyReached_whenReported_thenTheFrontIsWritten() throws IOException {
    // Arrange
    var observer = new SolveFrontObserver(frontFile(), 2, 500, false);

    // Act
    report(observer, 500, population(new double[] {0.0, 1.0}, new double[] {1.0, 0.0}));

    // Assert
    List<String> lines = Files.readAllLines(frontFile());
    assertEquals("Run,Evaluations,NonDominated,F1,F2", lines.get(0));
    assertEquals(3, lines.size());
    assertTrue(lines.contains("2,500,1,0.0,1.0"));
    assertTrue(lines.contains("2,500,1,1.0,0.0"));
  }

  @Test
  @DisplayName("given dominated solutions, when reported, then only the non-dominated are written")
  void givenDominatedSolutions_whenReported_thenOnlyTheNonDominatedAreWritten()
      throws IOException {
    // Arrange
    var observer = new SolveFrontObserver(frontFile(), 1, 100, false);

    // Act: (0.8, 0.8) is dominated by (0.5, 0.5)
    report(
        observer,
        100,
        population(new double[] {0.5, 0.5}, new double[] {0.8, 0.8}, new double[] {0.2, 0.9}));

    // Assert
    List<String> lines = Files.readAllLines(frontFile());
    assertEquals(3, lines.size());
    assertFalse(lines.contains("1,100,1,0.8,0.8"));
  }

  @Test
  @DisplayName("given a later report, when written, then the file holds only the latest front")
  void givenALaterReport_whenWritten_thenTheFileHoldsOnlyTheLatestFront() throws IOException {
    // Arrange
    var observer = new SolveFrontObserver(frontFile(), 1, 100, false);
    report(observer, 100, population(new double[] {0.9, 0.9}));

    // Act
    report(observer, 200, population(new double[] {0.1, 0.1}));

    // Assert
    List<String> lines = Files.readAllLines(frontFile());
    assertEquals(List.of("Run,Evaluations,NonDominated,F1,F2", "1,200,1,0.1,0.1"), lines);
    assertFalse(Files.exists(tempDir.resolve(SolveFrontObserver.FILE_NAME + ".tmp")));
  }

  @Test
  @DisplayName("given steps that do not divide the frequency, when reported, then it still writes")
  void givenStepsThatDoNotDivideTheFrequency_whenReported_thenItStillWrites() throws IOException {
    // Arrange: an algorithm evaluating 100 solutions at a time, a frequency of 250
    var observer = new SolveFrontObserver(frontFile(), 1, 250, false);

    // Act
    report(observer, 100, population(new double[] {0.5, 0.5}));
    report(observer, 200, population(new double[] {0.5, 0.5}));
    report(observer, 300, population(new double[] {0.5, 0.5}));

    // Assert: written at 300, not before
    assertEquals("1,300,1,0.5,0.5", Files.readAllLines(frontFile()).get(1));
  }

  @Test
  @DisplayName("given wholePopulation, when reported, then the dominated solutions are written too")
  void givenWholePopulation_whenReported_thenTheDominatedSolutionsAreWrittenToo()
      throws IOException {
    // Arrange
    var observer = new SolveFrontObserver(frontFile(), 1, 100, true);

    // Act: (0.8, 0.8) is dominated by (0.5, 0.5)
    report(
        observer,
        100,
        population(new double[] {0.5, 0.5}, new double[] {0.8, 0.8}, new double[] {0.2, 0.9}));

    // Assert
    List<String> lines = Files.readAllLines(frontFile());
    assertEquals(4, lines.size());
    assertTrue(lines.contains("1,100,1,0.5,0.5"));
    assertTrue(lines.contains("1,100,1,0.2,0.9"));
    assertTrue(lines.contains("1,100,0,0.8,0.8"));
  }
}
