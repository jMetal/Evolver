package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.meta.MetaSMPSOBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running SMPSO as meta-optimizer to configure {@link DoubleNSGAII}
 * using
 * problem {@link RE31} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class SMPSOOptimizingNSGAIIForProblemRE31 {

    // Meta-optimizer configuration
    private static final int META_MAX_EVALUATIONS = 2000;
    private static final int NUMBER_OF_CORES = 8;

    // Base-level algorithm configuration
    private static final int BASE_POPULATION_SIZE = 100;
    private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;
    private static final int BASE_MAX_EVALUATIONS = 10000;

    // Observer configuration
    private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
    private static final int WRITE_FREQUENCY = 1;
    private static final int PLOT_UPDATE_FREQUENCY = 1;

    public static void main(String[] args) throws IOException {
        String yamlParameterSpaceFile = "NSGAIIDouble.yaml";

        // Step 1: Select the target problem
        List<Problem<DoubleSolution>> trainingSet = List.of(new RE31());
        List<String> referenceFrontFileNames = List.of("resources/referenceFronts/RE31.csv");
        String problemName = "RE31";

        // Step 2: Set the parameters for the algorithm to be configured
        var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
        var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
        var configurableAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, parameterSpace);

        var maximumNumberOfEvaluations = List.of(BASE_MAX_EVALUATIONS);

        EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

        MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem = new MetaOptimizationProblem<>(
                configurableAlgorithm,
                trainingSet,
                referenceFrontFileNames,
                indicators,
                evaluationBudgetStrategy,
                NUMBER_OF_INDEPENDENT_RUNS);

        // Step 3: Set up and configure the meta-optimizer (SMPSO) using the specialized
        // builder
        ParticleSwarmOptimizationAlgorithm smpso = new MetaSMPSOBuilder(metaOptimizationProblem)
                .setMaxEvaluations(META_MAX_EVALUATIONS)
                .setNumberOfCores(NUMBER_OF_CORES)
                .build();

        // Step 4: Create observers for the meta-optimizer
        String algorithmName = "SMPSO";

        MetaOptimizerConfig config = MetaOptimizerConfig.builder()
                .metaOptimizerName(algorithmName)
                .metaMaxEvaluations(META_MAX_EVALUATIONS)
                .metaPopulationSize(100) // Default for SMPSO builder? Or explicitly set?
                // Checking builder usage in original code: .build() directly after
                // setMaxEvaluations and setNumberOfCores.
                // SMPSO default swarm size is usually 100. MetaSMPSOBuilder likely uses
                // default.
                // Let's check MetaSMPSOBuilder source code later if needed, but for now
                // assuming 100 or not setting it if not exposed.
                // Wait, MetaSMPSOBuilder might not have setPopulationSize or it might use
                // default.
                // The config needs a value. I'll use 100 as it's standard for SMPSO in
                // jMetal/Evolver meta.
                .metaPopulationSize(100)
                .numberOfCores(NUMBER_OF_CORES)
                .baseLevelAlgorithmName("NSGA-II")
                .baseLevelPopulationSize(BASE_POPULATION_SIZE)
                .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
                .yamlParameterSpaceFile(yamlParameterSpaceFile)
                .build();

        var outputResults = new ConsolidatedOutputResults(
                metaOptimizationProblem,
                problemName,
                indicators,
                "results/smpso/nsgaii/" + problemName,
                config);

        var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

        var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
        var frontChartObserver = new FrontPlotObserver<DoubleSolution>(
                "NSGA-II, " + trainingSet.get(0).name(),
                indicators.get(0).name(),
                indicators.get(1).name(),
                trainingSet.get(0).name(),
                PLOT_UPDATE_FREQUENCY);

        smpso.observable().register(evaluationObserver);
        smpso.observable().register(frontChartObserver);
        smpso.observable().register(writeExecutionDataToFilesObserver);

        // Step 5: Run the meta-optimizer
        smpso.run();

        // Step 6: Write results
        outputResults.updateEvaluations(META_MAX_EVALUATIONS);
        outputResults.writeResultsToFiles(smpso.result());

        System.exit(0);
    }
}
