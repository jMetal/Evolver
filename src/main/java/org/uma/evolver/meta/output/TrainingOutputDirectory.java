package org.uma.evolver.meta.output;

import java.io.File;
import java.util.List;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * The output directory of a training, shared by {@link ConsolidatedOutputResults} and {@link
 * TreeOutputResults}. Their files are appended to at each checkpoint, so a training written to the
 * directory of a previous one would mix both in the same files; a directory that already has the
 * results of a training is therefore rejected.
 */
final class TrainingOutputDirectory {

  /** The files that the output writers produce. */
  static final List<String> RESULT_FILES =
      List.of(
          "METADATA.txt",
          "INDICATORS.csv",
          "CONFIGURATIONS.csv",
          "VAR_CONF.txt",
          "POPULATION_INDICATORS.csv",
          "POPULATION_CONFIGURATIONS.csv");

  private TrainingOutputDirectory() {}

  /**
   * Creates the output directory of a training if it does not exist. Other files in it, such as the
   * {@code status.yaml} of {@code TrainingRunner}, are allowed.
   *
   * @param outputDirectoryName the output directory
   * @throws JMetalException if the directory cannot be created, or if it already has one of the
   *     {@link #RESULT_FILES}
   */
  static void prepare(String outputDirectoryName) {
    File outputDirectory = new File(outputDirectoryName);
    if (!outputDirectory.exists()) {
      if (!outputDirectory.mkdirs()) {
        throw new JMetalException("Error creating directory " + outputDirectoryName);
      }
      return;
    }
    List<String> existing =
        RESULT_FILES.stream().filter(name -> new File(outputDirectory, name).exists()).toList();
    if (!existing.isEmpty()) {
      throw new JMetalException(
          "The output directory "
              + outputDirectoryName
              + " already has the results of a training ("
              + String.join(", ", existing)
              + "): move or delete them, or choose another directory");
    }
  }
}
