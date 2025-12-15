package org.uma.evolver.example.ablation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.ablation.AblationResult;
import org.uma.evolver.ablation.AblationAnalyzer;
import org.uma.evolver.ablation.AblationAnalyzer.ProblemWithReferenceFront;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Example demonstrating multi-problem ablation analysis for NSGA-II using a training set of ZDT
 * problems.
 *
 * <p>This example evaluates parameter contributions across ZDT1, ZDT2, ZDT3, ZDT4, and ZDT6, which
 * is a common training set for automatic algorithm configuration experiments.
 *
 * @author Antonio J. Nebro
 */
public class MultiProblemAblationAnalysisExample {

  public static void main(String[] args) throws IOException {
    // Create problems with their reference fronts
    List<ProblemWithReferenceFront<DoubleSolution>> problems = createProblemSet();

    // Quality indicators
    List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());

    // Create the configurable algorithm template
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    DoubleProblem templateProblem = new ZDT1();
    int populationSize = 100;
    int maxEvaluations = 25000;

    // Initialize the parameter space
    YAMLParameterSpace parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());

    DoubleNSGAII algorithm =
        new DoubleNSGAII(templateProblem, populationSize, maxEvaluations, parameterSpace);

    // Create multi-problem ablation analyzer
    int numberOfRuns = 3; // Reduced for demonstration; use more in production

    AblationAnalyzer<DoubleSolution> analyzer =
        new AblationAnalyzer<>(
            algorithm, problems, indicators, maxEvaluations, numberOfRuns, parameterSpace);

    // Define default configuration (typical NSGA-II defaults)
    String defaultConfigString =
        "--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2";
    Map<String, String> defaultConfig = parseConfiguration(defaultConfigString);

    // Define optimized configuration (example: from meta-optimization on ZDT
    // training set)
    String optimizedConfigString =
        "--algorithmResult externalArchive --populationSizeWithArchive 20 --archiveType crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.6729893472234677 --crossoverRepairStrategy random --sbxDistributionIndex 16.497600223910695 --mutation linkedPolynomial --mutationProbabilityFactor 0.9913896382476491 --mutationRepairStrategy bounds --linkedPolynomialMutationDistributionIndex 9.247660639092558 --selection tournament --selectionTournamentSize 3 ";
    Map<String, String> optimizedConfig = parseConfiguration(optimizedConfigString);

    System.out.println("=== Multi-Problem Ablation Analysis for NSGA-II ===");
    System.out.println("Training set: ZDT1, ZDT2, ZDT3, ZDT4, ZDT6");
    System.out.println("Number of runs per configuration per problem: " + numberOfRuns);
    System.out.println();

    // Perform leave-one-out analysis
    System.out.println("Performing leave-one-out analysis...");
    System.out.println("(This may take several minutes)\n");

    AblationResult looResult = analyzer.leaveOneOutAnalysis(defaultConfig, optimizedConfig);

    System.out.println("=== LEAVE-ONE-OUT ANALYSIS RESULTS ===");
    System.out.println(looResult);

    // Perform forward path analysis
    System.out.println("\nPerforming forward path analysis...");
    System.out.println("(This may take several minutes)\n");

    AblationResult forwardResult = analyzer.forwardPathAnalysis(defaultConfig, optimizedConfig);

    System.out.println("=== FORWARD PATH ANALYSIS RESULTS ===");
    System.out.println(forwardResult);

    // Export to CSV
    String csvFilename = "ablation_results_multi_problem_ZDT.csv";
    try (FileWriter writer = new FileWriter(csvFilename)) {
      writer.write(looResult.toCSV());
    }
    System.out.println("\nResults exported to: " + csvFilename);
  }

  /** Creates the training set of ZDT problems with their reference fronts. */
  private static List<ProblemWithReferenceFront<DoubleSolution>> createProblemSet()
      throws IOException {

    List<ProblemWithReferenceFront<DoubleSolution>> problems = new ArrayList<>();
    String basePath = "resources/referenceFronts/";

    // ZDT1
    problems.add(
        new ProblemWithReferenceFront<>(
            new ZDT1(), VectorUtils.readVectors(basePath + "ZDT1.csv", ","), "ZDT1"));

    // ZDT2
    problems.add(
        new ProblemWithReferenceFront<>(
            new ZDT2(), VectorUtils.readVectors(basePath + "ZDT2.csv", ","), "ZDT2"));

    // ZDT3
    problems.add(
        new ProblemWithReferenceFront<>(
            new ZDT3(), VectorUtils.readVectors(basePath + "ZDT3.csv", ","), "ZDT3"));

    // ZDT4
    problems.add(
        new ProblemWithReferenceFront<>(
            new ZDT4(), VectorUtils.readVectors(basePath + "ZDT4.csv", ","), "ZDT4"));

    // ZDT6
    problems.add(
        new ProblemWithReferenceFront<>(
            new ZDT6(), VectorUtils.readVectors(basePath + "ZDT6.csv", ","), "ZDT6"));

    return problems;
  }

  private static Map<String, String> parseConfiguration(String configurationLine) {
    Map<String, String> config = new LinkedHashMap<>();
    String[] params = configurationLine.trim().split("\\s+");

    for (int i = 0; i < params.length; i += 2) {
      if (params[i].startsWith("--")) {
        String key = params[i].substring(2);
        String value = params[i + 1];
        config.put(key, value);
      }
    }
    return config;
  }
}
