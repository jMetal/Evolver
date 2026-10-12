package org.uma.evolver.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The version of this build of Evolver (e.g. {@code 3.0}), read from {@code evolver.properties},
 * which Maven writes with the version of the project. Clients of the command line (such as
 * Evolver-Studio) get it from the manifest of {@code DescribeMain}, without parsing the name of the
 * jar.
 */
public final class EvolverVersion {

  /** The value when the file is missing, e.g. classes compiled outside Maven. */
  public static final String UNKNOWN = "unknown";

  private static final String VERSION = read();

  private EvolverVersion() {}

  /**
   * Returns the version of this build of Evolver.
   *
   * @return the version, or {@link #UNKNOWN} if it cannot be read
   */
  public static String get() {
    return VERSION;
  }

  private static String read() {
    InputStream resource =
        EvolverVersion.class.getClassLoader().getResourceAsStream("evolver.properties");
    if (resource == null) {
      return UNKNOWN;
    }
    try (InputStream stream = resource) {
      Properties properties = new Properties();
      properties.load(stream);
      String version = properties.getProperty("version", UNKNOWN).trim();
      return version.isEmpty() || version.startsWith("${") ? UNKNOWN : version;
    } catch (IOException e) {
      return UNKNOWN;
    }
  }
}
