package org.uma.evolver.example.analysis.ablation;

import java.io.IOException;
import org.uma.evolver.analysis.ablation.AblationConfiguration;
import org.uma.evolver.analysis.ablation.AblationRunner;

/**
 * Example demonstrating multi-problem ablation analysis for NSGA-II using a training set of ZDT
 * problems.
 *
 * <p>This example evaluates parameter contributions across ZDT1, ZDT2, ZDT3, ZDT4, and ZDT6, which
 * is a common training set for automatic algorithm configuration experiments.
 * 
 * <p>This version has been refactored to use the configuration-based approach with {@link AblationRunner}.
 * For a simpler version, see {@link SimpleZDTAblationExample}.
 *
 * @author Antonio J. Nebro
 */
public class ZDTAblationAnalysisExample {

  public static void main(String[] args) throws IOException {
    // Create ZDT configuration with optimized parameters
    AblationConfiguration config = AblationConfiguration.forZDTProblems()
        .outputPrefix("ablation_results_multi_problem_ZDT");
    
    // Parse command line arguments and override configuration
    config = parseCommandLineArguments(config, args);
    
    // Create and run the ablation analysis
    AblationRunner runner = new AblationRunner(config);
    runner.run();
  }

  /**
   * Parses command line arguments and updates the configuration accordingly.
   * 
   * @param config the base configuration to modify
   * @param args command line arguments
   * @return updated configuration
   */
  private static AblationConfiguration parseCommandLineArguments(AblationConfiguration config, String[] args) {
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--threads":
          if (i + 1 < args.length) {
            try {
              int threads = Integer.parseInt(args[i + 1]);
              config = config.numberOfThreads(Math.max(1, threads));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid thread count, using default");
            }
          }
          break;
          
        case "--sequential":
          config = config.numberOfThreads(1);
          break;
          
        case "--parallel":
          config = config.numberOfThreads(Runtime.getRuntime().availableProcessors());
          break;
          
        case "--no-progress":
          config = config.enableProgressReporting(false);
          break;
          
        case "--runs":
          if (i + 1 < args.length) {
            try {
              int runs = Integer.parseInt(args[i + 1]);
              config = config.numberOfRuns(Math.max(1, runs));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid run count, using default");
            }
          }
          break;
          
        case "--evaluations":
          if (i + 1 < args.length) {
            try {
              int evaluations = Integer.parseInt(args[i + 1]);
              config = config.maxEvaluations(Math.max(1000, evaluations));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid evaluation count, using default");
            }
          }
          break;
          
        case "--help":
        case "-h":
          printUsage();
          System.exit(0);
          break;
          
        default:
          if (args[i].startsWith("--")) {
            System.err.println("Warning: Unknown argument " + args[i]);
          }
          break;
      }
    }
    return config;
  }
  
  /**
   * Prints usage information.
   */
  private static void printUsage() {
    System.out.println("Usage: java ZDTAblationAnalysisExample [options]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --threads <n>      Number of threads to use (default: all available)");
    System.out.println("  --sequential       Force sequential execution (equivalent to --threads 1)");
    System.out.println("  --parallel         Use all available processors");
    System.out.println("  --no-progress      Disable real-time progress reporting");
    System.out.println("  --runs <n>         Number of runs per configuration per problem (default: 10)");
    System.out.println("  --evaluations <n>  Maximum evaluations per run (default: 20000)");
    System.out.println("  --help, -h         Show this help message");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  java ZDTAblationAnalysisExample");
    System.out.println("  java ZDTAblationAnalysisExample --threads 4 --runs 25");
    System.out.println("  java ZDTAblationAnalysisExample --sequential --no-progress");
  }
}
