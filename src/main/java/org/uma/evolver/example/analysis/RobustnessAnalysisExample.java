package org.uma.evolver.example.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * @author Antonio J. Nebro
 */
public class RobustnessAnalysisExample {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/RE21.csv";

    // Setup Problem
    var problem = new RE21();
    int populationSize = 100;
    int maxEvaluations = 25000;

    // Load reference front for indicators
    double[][] referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
    List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());

    // Optimized configuration (example)
    String optimizedConfigString =
        "--algorithmResult externalArchive --populationSizeWithArchive 133 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.97 --crossoverRepairStrategy random --sbxDistributionIndex 133.8 --mutation uniform --mutationProbabilityFactor 0.51 --mutationRepairStrategy random --uniformMutationPerturbation 0.23 --selection tournament --selectionTournamentSize 5";
    Map<String, String> optimizedConfig = parseConfiguration(optimizedConfigString);

    // Create Algorithm and Parameter Space
    YAMLParameterSpace parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(problem, populationSize, maxEvaluations, parameterSpace);

    // Create Analyzer
    // 50 evaluations per config to smooth noise, 30 perturbed samples
    int runsPerSample = 10; // Lower for fast example
    int nSamples = 20;
    double sigma = 0.05; // 5% perturbation

    var analyzer =
        new RobustnessAnalyzer<DoubleSolution>(
            baseAlgorithm,
            problem,
            parameterSpace,
            indicators,
            referenceFront,
            maxEvaluations,
            runsPerSample);

    System.out.println("Starting Local Robustness Analysis...");
    System.out.println("Config: " + optimizedConfig);
    System.out.println("Perturbation Sigma: " + sigma);
    System.out.println("Runs per sample: " + runsPerSample);

    var results = analyzer.analyze(optimizedConfig, nSamples, sigma);

    // Export
    String outputFile = "robustness_results_RE21.csv";
    analyzer.exportToCSV(results, Paths.get(outputFile));
    System.out.println("Results exported to: " + outputFile);
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
