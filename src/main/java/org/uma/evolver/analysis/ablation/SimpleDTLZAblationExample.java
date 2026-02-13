package org.uma.evolver.analysis.ablation;

/**
 * Minimal example for running a DTLZ 3D ablation analysis.
 */
public class SimpleDTLZAblationExample {

  /**
   * Entry point.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    AblationConfiguration baseConfig = AblationConfiguration.forDTLZProblems();
    var parseResult = AblationCliParser.parse(baseConfig, args);
    boolean shouldRun = true;

    if (parseResult.showHelp()) {
      if (!parseResult.errorMessage().isBlank()) {
        System.out.println(parseResult.errorMessage());
      }
      AblationCliParser.printUsage(SimpleDTLZAblationExample.class.getSimpleName());
      shouldRun = false;
    }

    if (shouldRun) {
      new AblationRunner(parseResult.configuration()).run();
    }
  }
}
