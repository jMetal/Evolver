package org.uma.evolver.cli.solving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uma.evolver.cli.RunStatusWriter;
import org.uma.jmetal.util.observable.impl.DefaultObservable;
import org.yaml.snakeyaml.Yaml;

@DisplayName("Unit tests for class SolveProgressObserver")
class SolveProgressObserverTest {

  @TempDir private Path tempDir;

  private Path statusFile() {
    return tempDir.resolve("status.yaml");
  }

  private void report(SolveProgressObserver observer, int evaluations) {
    observer.update(new DefaultObservable<>("test"), Map.of("EVALUATIONS", evaluations));
  }

  private int evaluationsDone() throws IOException {
    Map<String, Object> status = new Yaml().load(Files.readString(statusFile()));
    assertEquals("RUNNING", status.get("status"));
    return (int) status.get("evaluationsDone");
  }

  @Test
  @DisplayName("given fewer evaluations than the frequency, when reported, then nothing is written")
  void givenFewerEvaluationsThanTheFrequency_whenReported_thenNothingIsWritten() {
    // Arrange
    var observer = new SolveProgressObserver(new RunStatusWriter(statusFile()), 10000, 0, 500);

    // Act
    report(observer, 100);
    report(observer, 400);

    // Assert
    assertFalse(Files.exists(statusFile()));
  }

  @Test
  @DisplayName("given the frequency reached, when reported, then the evaluations are written")
  void givenTheFrequencyReached_whenReported_thenTheEvaluationsAreWritten() throws IOException {
    // Arrange
    var observer = new SolveProgressObserver(new RunStatusWriter(statusFile()), 10000, 0, 500);

    // Act
    report(observer, 500);

    // Assert
    assertEquals(500, evaluationsDone());
  }

  @Test
  @DisplayName(
      "given steps that are not a divisor of the frequency, when reported, then it still writes")
  void givenStepsThatDoNotDivideTheFrequency_whenReported_thenItStillWrites() throws IOException {
    // Arrange: an algorithm evaluating 100 solutions at a time, a frequency of 250
    var observer = new SolveProgressObserver(new RunStatusWriter(statusFile()), 10000, 0, 250);

    // Act
    report(observer, 100);
    report(observer, 200);
    report(observer, 300);

    // Assert: written at 300 (the first report at least 250 after the start), not at 100 or 200
    assertEquals(300, evaluationsDone());
  }

  @Test
  @DisplayName("given runs already finished, when reported, then the evaluations include them")
  void givenRunsAlreadyFinished_whenReported_thenTheEvaluationsIncludeThem() throws IOException {
    // Arrange: the second run of 5000 evaluations each
    var observer = new SolveProgressObserver(new RunStatusWriter(statusFile()), 10000, 5000, 500);

    // Act
    report(observer, 1000);

    // Assert
    assertEquals(6000, evaluationsDone());
  }

  @Test
  @DisplayName("given the last evaluation, when reported, then it is left to the final status")
  void givenTheLastEvaluation_whenReported_thenItIsLeftToTheFinalStatus() {
    // Arrange
    var observer = new SolveProgressObserver(new RunStatusWriter(statusFile()), 10000, 0, 500);

    // Act
    report(observer, 10000);

    // Assert
    assertFalse(Files.exists(statusFile()));
  }
}
