package org.uma.evolver.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for reading and accessing configurations from a text file containing a
 * configuration of a base-level metaheuristic per line. Each line in the file represents a single
 * configuration.
 *
 * @author Antonio J. Nebro
 */
public class ConfigurationFileReader {
  private final List<String> configurations;
  private final String filePath;

  /**
   * Creates a new ConfigurationFileReader for the specified file.
   *
   * @param filePath Path to the configuration file
   * @throws IOException if an I/O error occurs reading the file
   * @throws IllegalArgumentException if filePath is null or empty
   */
  public ConfigurationFileReader(String filePath) throws IOException {
    if (filePath == null || filePath.trim().isEmpty()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }
    this.filePath = filePath;
    this.configurations = new ArrayList<>();
    loadConfigurations();
  }

  /**
   * Loads configurations from the file into memory.
   *
   * @throws IOException if an I/O error occurs reading the file
   */
  private void loadConfigurations() throws IOException {
    // First try to load from src/main/resources
    Path resourcePath = Paths.get("src/main/resources", filePath);
    Path path;
    
    if (Files.exists(resourcePath)) {
      path = resourcePath;
    } else {
      // Fall back to the original path if not found in resources
      path = Paths.get(filePath);
      if (!Files.exists(path)) {
        throw new IOException("Configuration file not found in src/main/resources/" + filePath + " or at " + filePath);
      }
    }

    configurations.addAll(Files.readAllLines(path));
    // Remove any empty lines and trim whitespace
    configurations.removeIf(line -> line.trim().isEmpty());
    // Trim whitespace from each line
    configurations.replaceAll(String::trim);
  }

  /**
   * Returns the total number of configurations in the file.
   *
   * @return number of configurations
   */
  public int getNumberOfConfigurations() {
    return configurations.size();
  }

  /**
   * Retrieves a configuration by its line number (1-based index).
   *
   * @param lineNumber the line number of the configuration to retrieve (starts at 1)
   * @return the configuration string
   * @throws IndexOutOfBoundsException if lineNumber is out of bounds
   */
  public String getConfiguration(int lineNumber) {
    if (lineNumber < 1 || lineNumber > configurations.size()) {
      throw new IndexOutOfBoundsException(
          String.format(
              "Line number %d is out of bounds. Valid range is 1 to %d",
              lineNumber, configurations.size()));
    }
    return configurations.get(lineNumber - 1);
  }

  /**
   * Gets all configurations as an unmodifiable list of strings.
   *
   * @return an unmodifiable list of all configurations
   */
  public List<String> getAllConfigurations() {
    return Collections.unmodifiableList(configurations);
  }

  /**
   * Gets the path of the configuration file.
   *
   * @return the file path
   */
  public String getFilePath() {
    return filePath;
  }
}
