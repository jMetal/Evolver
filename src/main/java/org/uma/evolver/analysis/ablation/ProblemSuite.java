package org.uma.evolver.analysis.ablation;

/**
 * Supported problem suites for ablation analyses.
 */
public enum ProblemSuite {
  ZDT("ZDT"),
  DTLZ("DTLZ"),
  WFG("WFG");

  private final String id;

  ProblemSuite(String id) {
    this.id = id;
  }

  /**
   * Returns the identifier used in configuration strings.
   *
   * @return suite identifier
   */
  public String id() {
    return id;
  }

  /**
   * Parses a problem suite from a string (case-insensitive).
   *
   * @param value the suite name
   * @return the matching {@code ProblemSuite}
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static ProblemSuite fromString(String value) {
    ProblemSuite result;
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Problem suite cannot be null or blank");
    }
    if ("ZDT".equalsIgnoreCase(value)) {
      result = ZDT;
    } else if ("DTLZ".equalsIgnoreCase(value)) {
      result = DTLZ;
    } else if ("WFG".equalsIgnoreCase(value)) {
      result = WFG;
    } else {
      throw new IllegalArgumentException("Unsupported problem suite: " + value);
    }
    return result;
  }
}
