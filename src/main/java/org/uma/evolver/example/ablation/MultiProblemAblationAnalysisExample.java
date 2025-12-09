package org.uma.evolver.example.ablation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.ablation.AblationResult;
import org.uma.evolver.ablation.MultiProblemAblationAnalyzer;
import org.uma.evolver.ablation.MultiProblemAblationAnalyzer.ProblemWithReferenceFront;
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
 * Example demonstrating multi-problem ablation analysis for NSGA-II
 * using a training set of ZDT problems.
 *
 * <p>This example evaluates parameter contributions across ZDT1, ZDT2, ZDT3, ZDT4, and ZDT6,
 * which is a common training set for automatic algorithm configuration experiments.</p>
 *
 * @author Antonio J. Nebro
 */
public class MultiProblemAblationAnalysisExample {

    public static void main(String[] args) throws IOException {
        // Create problems with their reference fronts
        List<ProblemWithReferenceFront<DoubleSolution>> problems = createProblemSet();

        // Quality indicators
        List<QualityIndicator> indicators = List.of(
                new Epsilon(),
                new NormalizedHypervolume()
        );

        // Create the configurable algorithm template
        String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
        DoubleProblem templateProblem = new ZDT1();
        int populationSize = 100;
        int maxEvaluations = 25000;
        
        DoubleNSGAII algorithm = new DoubleNSGAII(
                templateProblem,
                populationSize,
                maxEvaluations,
                new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())
        );

        // Create multi-problem ablation analyzer
        int numberOfRuns = 3; // Reduced for demonstration; use more in production
        
        MultiProblemAblationAnalyzer<DoubleSolution> analyzer = 
                new MultiProblemAblationAnalyzer<>(
                        algorithm,
                        problems,
                        indicators,
                        maxEvaluations,
                        numberOfRuns
                );

        // Define default configuration (typical NSGA-II defaults)
        Map<String, String> defaultConfig = createDefaultConfiguration();

        // Define optimized configuration (example: from meta-optimization on ZDT training set)
        Map<String, String> optimizedConfig = createOptimizedConfiguration();

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

    /**
     * Creates the training set of ZDT problems with their reference fronts.
     */
    private static List<ProblemWithReferenceFront<DoubleSolution>> createProblemSet() 
            throws IOException {
        
        List<ProblemWithReferenceFront<DoubleSolution>> problems = new ArrayList<>();
        String basePath = "resources/referenceFronts/";

        // ZDT1
        problems.add(new ProblemWithReferenceFront<>(
                new ZDT1(),
                VectorUtils.readVectors(basePath + "ZDT1.csv", ","),
                "ZDT1"
        ));

        // ZDT2
        problems.add(new ProblemWithReferenceFront<>(
                new ZDT2(),
                VectorUtils.readVectors(basePath + "ZDT2.csv", ","),
                "ZDT2"
        ));

        // ZDT3
        problems.add(new ProblemWithReferenceFront<>(
                new ZDT3(),
                VectorUtils.readVectors(basePath + "ZDT3.csv", ","),
                "ZDT3"
        ));

        // ZDT4
        problems.add(new ProblemWithReferenceFront<>(
                new ZDT4(),
                VectorUtils.readVectors(basePath + "ZDT4.csv", ","),
                "ZDT4"
        ));

        // ZDT6
        problems.add(new ProblemWithReferenceFront<>(
                new ZDT6(),
                VectorUtils.readVectors(basePath + "ZDT6.csv", ","),
                "ZDT6"
        ));

        return problems;
    }

    /**
     * Creates the default NSGA-II configuration.
     */
    private static Map<String, String> createDefaultConfiguration() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("populationSize", "100");
        config.put("offspringPopulationSize", "100");
        config.put("algorithmResult", "population");
        config.put("createInitialSolutions", "random");
        config.put("variation", "crossoverAndMutationVariation");
        config.put("selection", "tournament");
        config.put("selectionTournamentSize", "2");
        config.put("crossover", "SBX");
        config.put("crossoverProbability", "0.9");
        config.put("sbxDistributionIndex", "20.0");
        config.put("mutation", "polynomial");
        config.put("mutationProbabilityFactor", "1.0");
        config.put("polynomialMutationDistributionIndex", "20.0");
        return config;
    }

    /**
     * Creates an example optimized configuration.
     * In practice, this would come from a meta-optimization process.
     */
    private static Map<String, String> createOptimizedConfiguration() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("populationSize", "50");
        config.put("offspringPopulationSize", "50");
        config.put("algorithmResult", "externalArchive");
        config.put("populationSizeWithArchive", "50");
        config.put("archiveType", "crowdingDistanceArchive");
        config.put("createInitialSolutions", "latinHypercubeSampling");
        config.put("variation", "crossoverAndMutationVariation");
        config.put("selection", "tournament");
        config.put("selectionTournamentSize", "4");
        config.put("crossover", "SBX");
        config.put("crossoverProbability", "0.95");
        config.put("sbxDistributionIndex", "15.0");
        config.put("mutation", "uniform");
        config.put("mutationProbabilityFactor", "0.8");
        config.put("uniformMutationPerturbation", "0.3");
        return config;
    }
}
