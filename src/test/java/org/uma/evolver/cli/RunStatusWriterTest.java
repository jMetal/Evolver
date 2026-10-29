package org.uma.evolver.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Unit tests for class RunStatusWriter")
class RunStatusWriterTest {

  @TempDir private Path tempDir;

  @Test
  @DisplayName(
      "Given a status file in a directory that does not exist yet, when a failure is written, then"
          + " the directory is created and the file has the error")
  void givenAMissingDirectory_whenAFailureIsWritten_thenTheFileHasTheError() throws IOException {
    // Arrange: a request that fails before the run creates its output directory
    Path statusFile = tempDir.resolve("results/run01/status.yaml");
    var writer = new RunStatusWriter(statusFile);

    // Act
    writer.write(RunStatusWriter.State.FAILED, 0, 2000, "Missing parameter: --crossoverProbability");

    // Assert
    String status = Files.readString(statusFile);
    assertTrue(status.contains("FAILED"), status);
    assertTrue(status.contains("Missing parameter: --crossoverProbability"), status);
  }
}
