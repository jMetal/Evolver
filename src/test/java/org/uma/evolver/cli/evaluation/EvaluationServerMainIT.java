package org.uma.evolver.cli.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The command-line utility of the evaluation server, with the server in a process of its own. */
@Tag("integration")
@DisplayName("Integration tests for class EvaluationServerMain")
class EvaluationServerMainIT {

  @TempDir Path stateDir;

  private static int freePort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Test
  @DisplayName(
      "given a free port, when the server is started, inspected and stopped, then each command"
          + " succeeds and the state file follows the server")
  void givenAFreePort_whenStartedInspectedAndStopped_thenEachCommandSucceeds() throws Exception {
    // Arrange
    String port = String.valueOf(freePort());
    String[] common = {"--port", port, "--state-dir", stateDir.toString()};
    Path stateFile = stateDir.resolve("server-" + port + ".yaml");

    // Act & Assert
    assertEquals(1, EvaluationServerMain.execute(join("status", common)));
    assertEquals(
        0,
        EvaluationServerMain.execute(
            join("start", "TutorialIraceZdtBaseLevel.yaml", "--threads", "2", common)));
    assertTrue(Files.exists(stateFile));
    assertTrue(Files.readString(stateFile).contains("port: " + port));
    assertEquals(0, EvaluationServerMain.execute(join("status", common)));
    assertEquals(
        1, EvaluationServerMain.execute(join("start", "TutorialIraceZdtBaseLevel.yaml", common)));
    assertEquals(0, EvaluationServerMain.execute(join("stop", common)));
    assertFalse(Files.exists(stateFile));
    assertEquals(1, EvaluationServerMain.execute(join("status", common)));
  }

  @Test
  @DisplayName("given no server, when stopped, then it says so with exit code 1")
  void givenNoServer_whenStopped_thenItSaysSo() throws Exception {
    String port = String.valueOf(freePort());
    assertEquals(
        1,
        EvaluationServerMain.execute(
            new String[] {"stop", "--port", port, "--state-dir", stateDir.toString()}));
  }

  private static String[] join(Object... parts) {
    return java.util.Arrays.stream(parts)
        .flatMap(
            part ->
                part instanceof String[] array
                    ? java.util.Arrays.stream(array)
                    : java.util.stream.Stream.of((String) part))
        .toArray(String[]::new);
  }
}
