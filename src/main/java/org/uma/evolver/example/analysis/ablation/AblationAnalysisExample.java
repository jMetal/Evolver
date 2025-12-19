package org.uma.evolver.example.analysis.ablation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.analysis.ablation.AblationAnalyzer;
import org.uma.evolver.analysis.ablation.AblationAnalyzer.ProblemWithReferenceFront;
import org.uma.evolver.analysis.ablation.AblationResult;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Example demonstrating ablation analysis on NSGA-II configurations for the
 * RE21 (Four-bar truss
 * design) problem.
 *
 * <p>
 * This example shows how to use the AblationAnalyzer (configured for a single
 * problem) to
 * understand the contribution of individual parameter changes from a default
 * configuration to an
 * optimized one.
 *
 * <p>
 * The ablation analysis helps answer questions like:
 *
 * <ul>
 * <li>Which parameters contribute most to the performance improvement?
 * <li>Are there parameters that can be reverted to default without significant
 * loss?
 * <li>What is the order of importance of parameter changes?
 * </ul>
 *
 * @author Antonio J. Nebro
 */
public class AblationAnalysisExample {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/RE21.csv";

    // Problem and algorithm setup
    var problem = new RE21();
    int populationSize = 100;
    int maxEvaluations = 25000;
    int numberOfRuns = 5;

    // Load reference front
    double[][] referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");

    // Quality indicators
    List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());

    // Default NSGA-II configuration
    String defaultConfigString = "--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2";
    Map<String, String> defaultConfig = parseConfiguration(defaultConfigString);

    // Optimized configuration
    String optimizedConfigString = "--algorithmResult externalArchive --populationSizeWithArchive 133 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.97 --crossoverRepairStrategy random --sbxDistributionIndex 133.8 --mutation uniform --mutationProbabilityFactor 0.51 --mutationRepairStrategy random --uniformMutationPerturbation 0.23 --selection tournament --selectionTournamentSize 5";
    Map<String, String> optimizedConfig = parseConfiguration(optimizedConfigString);

    // Create the base algorithm template
    YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(problem, populationSize, maxEvaluations, parameterSpace);

    // Prepare problem wrapper for AblationAnalyzer
    var problemWithRefFront = new ProblemWithReferenceFront<>(problem, referenceFront, "RE21");

    // Create analyzer
    var analyzer = new AblationAnalyzer<DoubleSolution>(
        baseAlgorithm,
        List.of(problemWithRefFront),
        indicators,
        maxEvaluations,
        numberOfRuns,
        parameterSpace);

    System.out.println("╔══════════════════════════════════════════════════════════════╗");
    System.out.println("║           ABLATION ANALYSIS FOR NSGA-II ON RE21              ║");
    System.out.println("╠══════════════════════════════════════════════════════════════╣");
    System.out.println("║ Problem: RE21 (Four-bar truss design)                        ║");
    System.out.println(
        "║ Max Evaluations: " + maxEvaluations + "                                    ║");
    System.out.println(
        "║ Number of Runs: " + numberOfRuns + "                                         ║");
    System.out.println("╚══════════════════════════════════════════════════════════════╝");
    System.out.println();

    System.out.println("Starting Leave-One-Out Ablation Analysis...");
    System.out.println("This may take several minutes.\n");

    // Perform leave-one-out analysis
    AblationResult looResult = analyzer.leaveOneOutAnalysis(defaultConfig, optimizedConfig);
    System.out.println(looResult);

    // Export to CSV
    String csvOutput = looResult.toCSV();
    Files.writeString(Paths.get("ablation_results_RE21.csv"), csvOutput);
    System.out.println("Results exported to ablation_results_RE21.csv\n");

    System.out.println("Starting Forward Path Ablation Analysis...\n");

    // Perform forward path analysis
    // Perform forward path analysis
    AblationResult pathResult = analyzer.forwardPathAnalysis(defaultConfig, optimizedConfig);
    System.out.println(pathResult);

    // Export Path to CSV
    String pathCsvOutput = pathResult.pathToCSV();
    Files.writeString(Paths.get("ablation_path_RE21.csv"), pathCsvOutput);
    System.out.println("Path results exported to ablation_path_RE21.csv\n");
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
