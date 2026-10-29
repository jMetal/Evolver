package org.uma.evolver.meta.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for the output directory of a training")
class TrainingOutputDirectoryTest {

  @TempDir private Path tempDir;

  @Nested
  @DisplayName("When the output directory is prepared")
  class Prepare {

    @Test
    @DisplayName("Given a directory that does not exist, when prepared, then it is created")
    void givenAMissingDirectory_whenPrepared_thenItIsCreated() {
      // Arrange
      Path directory = tempDir.resolve("a/b");

      // Act
      TrainingOutputDirectory.prepare(directory.toString());

      // Assert
      assertTrue(Files.isDirectory(directory));
    }

    @Test
    @DisplayName(
        "Given a directory with other files, such as status.yaml, when prepared, then it is"
            + " accepted")
    void givenADirectoryWithOtherFiles_whenPrepared_thenItIsAccepted() throws IOException {
      // Arrange
      Files.writeString(tempDir.resolve("status.yaml"), "state: RUNNING\n");
      Files.writeString(tempDir.resolve("request.yaml"), "outputDirectory: x\n");

      // Act & Assert
      assertDoesNotThrow(() -> TrainingOutputDirectory.prepare(tempDir.toString()));
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("org.uma.evolver.meta.output.TrainingOutputDirectory#RESULT_FILES")
    @DisplayName(
        "Given a directory with a result file of a previous training, when prepared, then it is"
            + " rejected")
    void givenADirectoryWithAResultFile_whenPrepared_thenItIsRejected(String fileName)
        throws IOException {
      // Arrange
      Files.writeString(tempDir.resolve(fileName), "");

      // Act & Assert
      var exception =
          assertThrows(
              JMetalException.class, () -> TrainingOutputDirectory.prepare(tempDir.toString()));
      assertTrue(exception.getMessage().contains(fileName), exception.getMessage());
    }
  }
}
