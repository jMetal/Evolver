package org.uma.evolver.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
 * <p>An absolute path is read as it is. A relative one, such as {@code
 * defaultConfigurations/NSGAIIDoubleDefault.txt}, is looked up in {@code src/main/resources}, then
 * in the working directory and then on the classpath, so the bundled configurations are also found
 * when Evolver runs from a jar, outside the project directory.
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

  /** The lines of a classpath resource, or null if there is none with that name. */
  private static List<String> readFromClasspath(String name) throws IOException {
    String resource = name.replace('\\', '/');
    try (InputStream stream =
        ConfigurationFileReader.class.getClassLoader().getResourceAsStream(resource)) {
      if (stream == null) {
        return null;
      }
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        return reader.lines().toList();
      }
    }
  }

  /**
   * Loads configurations from the file into memory.
   *
   * @throws IOException if an I/O error occurs reading the file
   */
  private void loadConfigurations() throws IOException {
    Path path = Paths.get(filePath);
    // A relative path is looked up first in src/main/resources. An absolute one is used as it is:
    // joined to another path, a Windows one ("C:\...") is not even a valid path.
    if (!path.isAbsolute()) {
      Path resourcePath = Paths.get("src", "main", "resources").resolve(path);
      if (Files.exists(resourcePath)) {
        path = resourcePath;
      }
    }

    if (Files.exists(path)) {
      configurations.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
    } else {
      // Outside the project directory (from a jar, for instance) the bundled files are on the
      // classpath
      List<String> lines = path.isAbsolute() ? null : readFromClasspath(filePath);
      if (lines == null) {
        throw new IOException(
            "Configuration file not found in src/main/resources/"
                + filePath
                + ", at "
                + filePath
                + " or on the classpath");
      }
      configurations.addAll(lines);
    }
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
