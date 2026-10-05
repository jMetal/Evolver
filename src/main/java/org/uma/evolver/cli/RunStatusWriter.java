package org.uma.evolver.cli;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.uma.evolver.util.JMetalExceptions;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

/** Writes the progress of a training or solve run to a YAML status file, so an external
 * process (e.g. a GUI) can poll it instead of parsing log output. */
public final class RunStatusWriter {

  public enum State {
    RUNNING,
    FINISHED,
    FAILED
  }

  private final Path statusFile;
  private Double maxComputingTimeMinutes;
  private long startMillis;

  public RunStatusWriter(Path statusFile) {
    this.statusFile = statusFile;
  }

  /**
   * Declares that the run is bounded by computing time: from now on the status file also has the
   * limit ({@code maxComputingTimeMinutes}) and the time elapsed since this call ({@code
   * elapsedMinutes}), and {@code maxEvaluations} is 0 (there is no limit on the evaluations).
   *
   * @return this writer
   */
  public RunStatusWriter limitComputingTime(double minutes) {
    this.maxComputingTimeMinutes = minutes;
    this.startMillis = System.currentTimeMillis();
    return this;
  }

  public void write(State state, int evaluationsDone, int maxEvaluations) {
    write(state, evaluationsDone, maxEvaluations, null);
  }

  public void write(State state, int evaluationsDone, int maxEvaluations, String errorMessage) {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("status", state.name());
    status.put("evaluationsDone", evaluationsDone);
    status.put("maxEvaluations", maxEvaluations);
    if (maxComputingTimeMinutes != null) {
      status.put("maxComputingTimeMinutes", maxComputingTimeMinutes);
      status.put(
          "elapsedMinutes",
          Math.round((System.currentTimeMillis() - startMillis) / 600.0) / 100.0);
    }
    status.put("updatedAt", LocalDateTime.now().toString());
    if (errorMessage != null) {
      status.put("errorMessage", errorMessage);
    }

    try (FileWriter writer = new FileWriter(statusFile.toFile())) {
      new Yaml().dump(status, writer);
    } catch (IOException e) {
      throw JMetalExceptions.withCause("Error writing status file: " + statusFile, e);
    }
  }
}
