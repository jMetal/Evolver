package org.uma.evolver.analysis.ablation;

/**
 * Parses command-line arguments for ablation example runners.
 */
public final class AblationCliParser {

  private AblationCliParser() {
    // Utility class
  }

  /**
   * Parses command-line arguments and applies them to a configuration.
   *
   * @param configuration base configuration
   * @param args command-line arguments
   * @return parsed result
   */
  public static CliParseResult parse(AblationConfiguration configuration, String[] args) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }

    AblationConfiguration updated = configuration;
    boolean showHelp = false;
    String error = "";

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if ("--help".equals(arg) || "-h".equals(arg)) {
        showHelp = true;
      } else if ("--threads".equals(arg)) {
        if (i + 1 >= args.length) {
          error = "Missing value for --threads";
          showHelp = true;
        } else {
          try {
            int value = Integer.parseInt(args[i + 1]);
            updated = updated.numberOfThreads(value);
            i++;
          } catch (NumberFormatException e) {
            error = "Invalid integer for --threads: " + args[i + 1];
            showHelp = true;
          }
        }
      } else if ("--runs".equals(arg)) {
        if (i + 1 >= args.length) {
          error = "Missing value for --runs";
          showHelp = true;
        } else {
          try {
            int value = Integer.parseInt(args[i + 1]);
            updated = updated.numberOfRuns(value);
            i++;
          } catch (NumberFormatException e) {
            error = "Invalid integer for --runs: " + args[i + 1];
            showHelp = true;
          }
        }
      } else if ("--evaluations".equals(arg)) {
        if (i + 1 >= args.length) {
          error = "Missing value for --evaluations";
          showHelp = true;
        } else {
          try {
            int value = Integer.parseInt(args[i + 1]);
            updated = updated.maxEvaluations(value);
            i++;
          } catch (NumberFormatException e) {
            error = "Invalid integer for --evaluations: " + args[i + 1];
            showHelp = true;
          }
        }
      } else if ("--sequential".equals(arg)) {
        updated = updated.numberOfThreads(1);
      } else if ("--parallel".equals(arg)) {
        updated = updated.numberOfThreads(Runtime.getRuntime().availableProcessors());
      } else if ("--no-progress".equals(arg)) {
        updated = updated.enableProgressReporting(false);
      } else {
        error = "Unknown option: " + arg;
        showHelp = true;
      }
    }

    return new CliParseResult(updated, showHelp, error);
  }

  /**
   * Prints usage instructions.
   *
   * @param className the example class name
   */
  public static void printUsage(String className) {
    System.out.println("Usage: java " + className + " [options]");
    System.out.println("Options:");
    System.out.println("  --threads <n>       Number of threads to use");
    System.out.println("  --runs <n>          Number of runs per configuration");
    System.out.println("  --evaluations <n>   Max evaluations per run");
    System.out.println("  --sequential        Force sequential execution");
    System.out.println("  --parallel          Use all available processors");
    System.out.println("  --no-progress       Disable progress reporting");
    System.out.println("  --help, -h          Show this help message");
  }

  /**
   * Result of CLI parsing.
   *
   * @param configuration updated configuration
   * @param showHelp whether help should be shown
   * @param errorMessage error message if parsing failed
   */
  public record CliParseResult(
      AblationConfiguration configuration,
      boolean showHelp,
      String errorMessage) {
  }
}
