package org.uma.evolver.example.ablation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.ablation.AblationAnalyzer;
import org.uma.evolver.ablation.AblationResult;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Example demonstrating ablation analysis on NSGA-II configurations
 * for the RE21 (Four-bar truss design) problem.
 *
 * <p>This example shows how to use the AblationAnalyzer to understand the contribution
 * of individual parameter changes from a default configuration to an optimized one.</p>
 *
 * <p>The ablation analysis helps answer questions like:
 * <ul>
 *   <li>Which parameters contribute most to the performance improvement?</li>
 *   <li>Are there parameters that can be reverted to default without significant loss?</li>
 *   <li>What is the order of importance of parameter changes?</li>
 * </ul>
 * </p>
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
        var indicators = List.of(new Epsilon(), new NormalizedHypervolume());

        // Default NSGA-II configuration
        Map<String, String> defaultConfig = new LinkedHashMap<>();
        defaultConfig.put("algorithmResult", "population");
        defaultConfig.put("createInitialSolutions", "default");
        defaultConfig.put("offspringPopulationSize", String.valueOf(populationSize));
        defaultConfig.put("variation", "crossoverAndMutationVariation");
        defaultConfig.put("crossover", "SBX");
        defaultConfig.put("crossoverProbability", "0.9");
        defaultConfig.put("crossoverRepairStrategy", "bounds");
        defaultConfig.put("sbxDistributionIndex", "20.0");
        defaultConfig.put("mutation", "polynomial");
        defaultConfig.put("mutationProbabilityFactor", "1.0");
        defaultConfig.put("mutationRepairStrategy", "bounds");
        defaultConfig.put("polynomialMutationDistributionIndex", "20.0");
        defaultConfig.put("selection", "tournament");
        defaultConfig.put("selectionTournamentSize", "2");

        // Optimized configuration (example - replace with actual optimized config from Evolver)
        Map<String, String> optimizedConfig = new LinkedHashMap<>();
        optimizedConfig.put("algorithmResult", "externalArchive");
        optimizedConfig.put("populationSizeWithArchive", "133");
        optimizedConfig.put("archiveType", "unboundedArchive");
        optimizedConfig.put("createInitialSolutions", "scatterSearch");
        optimizedConfig.put("offspringPopulationSize", "20");
        optimizedConfig.put("variation", "crossoverAndMutationVariation");
        optimizedConfig.put("crossover", "SBX");
        optimizedConfig.put("crossoverProbability", "0.97");
        optimizedConfig.put("crossoverRepairStrategy", "random");
        optimizedConfig.put("sbxDistributionIndex", "133.8");
        optimizedConfig.put("mutation", "uniform");
        optimizedConfig.put("mutationProbabilityFactor", "0.51");
        optimizedConfig.put("mutationRepairStrategy", "random");
        optimizedConfig.put("uniformMutationPerturbation", "0.23");
        optimizedConfig.put("selection", "tournament");
        optimizedConfig.put("selectionTournamentSize", "5");

        // Create the base algorithm template
        var baseAlgorithm = new DoubleNSGAII(
                problem,
                populationSize,
                maxEvaluations,
                new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())
        );

        // Create analyzer
        var analyzer = new AblationAnalyzer<DoubleSolution>(
                baseAlgorithm,
                problem,
                indicators,
                referenceFront,
                maxEvaluations,
                numberOfRuns
        );

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           ABLATION ANALYSIS FOR NSGA-II ON RE21              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║ Problem: RE21 (Four-bar truss design)                        ║");
        System.out.println("║ Max Evaluations: " + maxEvaluations + "                                    ║");
        System.out.println("║ Number of Runs: " + numberOfRuns + "                                         ║");
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
        AblationResult pathResult = analyzer.forwardPathAnalysis(defaultConfig, optimizedConfig);
        System.out.println(pathResult);
    }
}
