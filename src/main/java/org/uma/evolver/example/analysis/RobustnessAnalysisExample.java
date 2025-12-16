package org.uma.evolver.example.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.uma.evolver.analysis.RobustnessAnalyzer;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Example demonstrating robustness analysis on an optimized NSGA-II configuration.
 *
 * <p>This example evaluates the local robustness of an algorithm configuration by applying small
 * perturbations and measuring performance variation. It helps distinguish between robust "plateaus"
 * and fragile "peaks" in the parameter space.
 *
 * <p>Usage examples:
 * <ul>
 *   <li>{@code java RobustnessAnalysisExample} - Run with default settings
 *   <li>{@code java RobustnessAnalysisExample --samples 30} - Use 30 perturbed samples
 *   <li>{@code java RobustnessAnalysisExample --sigma 0.10} - Use 10% perturbation
 *   <li>{@code java RobustnessAnalysisExample --runs 15} - Use 15 runs per sample
 *   <li>{@code java RobustnessAnalysisExample --help} - Show help message
 * </ul>
 *
 * @author Antonio J. Nebro
 */
public class RobustnessAnalysisExample {

  public static void main(String[] args) throws IOException {
    // Parse command line arguments
    RobustnessConfig config = parseCommandLineArguments(args);
    
    if (config.showHelp) {
      printUsage();
      return;
    }

    System.out.println("=== Robustness Analysis for NSGA-II Configuration ===");
    System.out.println("Problem: RE21 (Real-world engineering problem)");
    System.out.println("Perturbation samples: " + config.nSamples);
    System.out.println("Perturbation sigma: " + (config.sigma * 100) + "%");
    System.out.println("Runs per sample: " + config.runsPerSample);
    System.out.println();

    try {
      // Setup problem and parameters
      String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
      String referenceFrontFileName = "resources/referenceFronts/RE21.csv";

      // Validate reference front file exists
      Path referenceFrontPath = Paths.get(referenceFrontFileName);
      if (!Files.exists(referenceFrontPath)) {
        System.err.println("Error: Reference front file not found: " + referenceFrontFileName);
        System.err.println("Please ensure the reference front file exists in the resources directory.");
        return;
      }

      // Setup Problem
      var problem = new RE21();
      int populationSize = 100;
      int maxEvaluations = config.maxEvaluations;

      // Load reference front for indicators
      double[][] referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
      List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());

      // Optimized configuration (from meta-optimization experiment)
      String optimizedConfigString =
          "--algorithmResult externalArchive --populationSizeWithArchive 133 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.97 --crossoverRepairStrategy random --sbxDistributionIndex 133.8 --mutation uniform --mutationProbabilityFactor 0.51 --mutationRepairStrategy random --uniformMutationPerturbation 0.23 --selection tournament --selectionTournamentSize 5";
      Map<String, String> optimizedConfig = parseConfiguration(optimizedConfigString);

      // Create Algorithm and Parameter Space
      YAMLParameterSpace parameterSpace =
          new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
      var baseAlgorithm = new DoubleNSGAII(problem, populationSize, maxEvaluations, parameterSpace);

      // Create Robustness Analyzer
      var analyzer =
          new RobustnessAnalyzer<DoubleSolution>(
              baseAlgorithm,
              problem,
              parameterSpace,
              indicators,
              referenceFront,
              maxEvaluations,
              config.runsPerSample);

      System.out.println("Starting robustness analysis...");
      System.out.println("Configuration under test:");
      optimizedConfig.forEach((key, value) -> 
          System.out.printf("  %s: %s%n", key, value));
      System.out.println();

      // Run robustness analysis
      long startTime = System.currentTimeMillis();
      var results = analyzer.analyze(optimizedConfig, config.nSamples, config.sigma);
      long analysisTime = System.currentTimeMillis() - startTime;

      // Export results
      String outputFile = config.outputFile;
      analyzer.exportToCSV(results, Paths.get(outputFile));
      
      // Analyze and report results
      analyzeRobustnessResults(results, indicators);
      
      System.out.println("\n=== Analysis Complete ===");
      System.out.printf("Total analysis time: %.2f seconds%n", analysisTime / 1000.0);
      System.out.println("Results exported to: " + outputFile);
      System.out.println("\nUse the exported CSV file for detailed statistical analysis and visualization.");

    } catch (IOException e) {
      System.err.println("Error during robustness analysis: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Unexpected error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Analyzes robustness results and provides statistical summary.
   */
  private static void analyzeRobustnessResults(List<Map<String, Object>> results, 
                                             List<QualityIndicator> indicators) {
    System.out.println("\n=== Robustness Analysis Results ===");
    
    // Separate baseline and perturbed results
    Map<String, Object> baseline = results.stream()
        .filter(r -> "Baseline".equals(r.get("Type")))
        .findFirst()
        .orElse(null);
    
    List<Map<String, Object>> perturbedResults = results.stream()
        .filter(r -> "Perturbed".equals(r.get("Type")))
        .collect(Collectors.toList());
    
    if (baseline == null || perturbedResults.isEmpty()) {
      System.err.println("Error: Invalid results structure");
      return;
    }
    
    // Analyze each indicator
    for (QualityIndicator indicator : indicators) {
      String indicatorName = indicator.name();
      
      double baselineValue = (Double) baseline.get(indicatorName);
      List<Double> perturbedValues = perturbedResults.stream()
          .map(r -> (Double) r.get(indicatorName))
          .collect(Collectors.toList());
      
      // Calculate statistics
      double mean = perturbedValues.stream().mapToDouble(d -> d).average().orElse(0);
      double variance = perturbedValues.stream()
          .mapToDouble(d -> Math.pow(d - mean, 2))
          .average().orElse(0);
      double stdDev = Math.sqrt(variance);
      double cv = Math.abs(stdDev / mean); // Coefficient of variation
      double degradation = (baselineValue - mean) / baselineValue;
      
      // Classify robustness
      String robustnessClass = classifyRobustness(cv, Math.abs(degradation));
      
      System.out.printf("\n--- %s ---\n", indicatorName);
      System.out.printf("Baseline Performance: %.6f\n", baselineValue);
      System.out.printf("Perturbed Mean: %.6f ± %.6f\n", mean, stdDev);
      System.out.printf("Coefficient of Variation: %.2f%%\n", cv * 100);
      System.out.printf("Performance Change: %+.2f%%\n", degradation * 100);
      System.out.printf("Robustness Classification: %s\n", robustnessClass);
    }
  }
  
  /**
   * Classifies configuration robustness based on coefficient of variation and performance degradation.
   */
  private static String classifyRobustness(double cv, double degradation) {
    if (cv < 0.05 && degradation < 0.02) {
      return "ROBUST (Low variation, minimal degradation)";
    } else if (cv < 0.15 && degradation < 0.05) {
      return "MODERATELY ROBUST (Acceptable variation and degradation)";
    } else if (cv < 0.25 || degradation < 0.10) {
      return "SOMEWHAT FRAGILE (Higher variation or degradation)";
    } else {
      return "FRAGILE (High variation and/or significant degradation)";
    }
  }

  /**
   * Configuration class for robustness analysis parameters.
   */
  private static class RobustnessConfig {
    int nSamples = 20;
    double sigma = 0.05; // 5% perturbation
    int runsPerSample = 10;
    int maxEvaluations = 25000;
    String outputFile = "robustness_results_RE21.csv";
    boolean showHelp = false;
  }

  /**
   * Parses command line arguments and returns configuration.
   */
  private static RobustnessConfig parseCommandLineArguments(String[] args) {
    RobustnessConfig config = new RobustnessConfig();
    
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--samples":
          if (i + 1 < args.length) {
            try {
              config.nSamples = Math.max(1, Integer.parseInt(args[i + 1]));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid sample count, using default");
            }
          }
          break;
          
        case "--sigma":
          if (i + 1 < args.length) {
            try {
              config.sigma = Math.max(0.001, Double.parseDouble(args[i + 1]));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid sigma value, using default");
            }
          }
          break;
          
        case "--runs":
          if (i + 1 < args.length) {
            try {
              config.runsPerSample = Math.max(1, Integer.parseInt(args[i + 1]));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid runs per sample, using default");
            }
          }
          break;
          
        case "--evaluations":
          if (i + 1 < args.length) {
            try {
              config.maxEvaluations = Math.max(1000, Integer.parseInt(args[i + 1]));
              i++; // Skip next argument
            } catch (NumberFormatException e) {
              System.err.println("Warning: Invalid evaluation count, using default");
            }
          }
          break;
          
        case "--output":
          if (i + 1 < args.length) {
            config.outputFile = args[i + 1];
            i++; // Skip next argument
          }
          break;
          
        case "--help":
        case "-h":
          config.showHelp = true;
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
    System.out.println("Usage: java RobustnessAnalysisExample [options]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --samples <n>      Number of perturbed samples to evaluate (default: 20)");
    System.out.println("  --sigma <f>        Perturbation standard deviation as fraction (default: 0.05)");
    System.out.println("  --runs <n>         Number of runs per sample for noise reduction (default: 10)");
    System.out.println("  --evaluations <n>  Maximum evaluations per run (default: 25000)");
    System.out.println("  --output <file>    Output CSV file name (default: robustness_results_RE21.csv)");
    System.out.println("  --help, -h         Show this help message");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  java RobustnessAnalysisExample");
    System.out.println("  java RobustnessAnalysisExample --samples 30 --sigma 0.10");
    System.out.println("  java RobustnessAnalysisExample --runs 15 --evaluations 50000");
    System.out.println();
    System.out.println("Perturbation Guidelines:");
    System.out.println("  --sigma 0.01-0.03  Very small perturbations (1-3%)");
    System.out.println("  --sigma 0.03-0.07  Standard robustness evaluation (3-7%)");
    System.out.println("  --sigma 0.07-0.15  Stress testing (7-15%)");
    System.out.println("  --sigma >0.15      Extreme robustness evaluation (>15%)");
    System.out.println();
    System.out.println("The analysis evaluates configuration stability by applying Gaussian");
    System.out.println("perturbations to numerical parameters and measuring performance variation.");
  }

  private static Map<String, String> parseConfiguration(String configurationLine) {
    Map<String, String> config = new LinkedHashMap<>();
    String[] params = configurationLine.trim().split("\\s+");
    for (int i = 0; i < params.length; i += 2) {
      if (params[i].startsWith("--")) {
        config.put(params[i].substring(2), params[i + 1]);
      }
    }
    return config;
  }
}
