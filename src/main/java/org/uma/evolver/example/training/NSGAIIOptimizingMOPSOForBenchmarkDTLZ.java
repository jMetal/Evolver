package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.mopso.BaseMOPSO;
import org.uma.evolver.meta.builder.MetaNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link BaseMOPSO}
 * using
 * problem DTLZ problems as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingMOPSOForBenchmarkDTLZ {

    // Meta-optimizer configuration
    private static final int META_MAX_EVALUATIONS = 100;
    private static final int META_POPULATION_SIZE = 100;
    private static final int NUMBER_OF_CORES = 8;

    // Base-level algorithm configuration
    private static final int BASE_POPULATION_SIZE = 100;
    private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;
    private static final int BASE_MAX_EVALUATIONS = 250;

    // Observer configuration
    private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
    private static final int WRITE_FREQUENCY = 10;
    private static final int PLOT_UPDATE_FREQUENCY = 10;

    public static void main(String[] args) throws IOException {
        String yamlParameterSpaceFile = "MOPSO.yaml";

        // Step 1: Select the target problem
        TrainingSet<DoubleSolution> trainingSetDescriptor = new DTLZ3DTrainingSet();

        List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
        List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();

        // Step 2: Set the parameters for the algorithm to be configured
        var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
        var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new MOPSOParameterFactory());

        var baseAlgorithm = new BaseMOPSO(BASE_POPULATION_SIZE, parameterSpace);
        // Use small values for quick test
        var maximumNumberOfEvaluations = java.util.Collections.nCopies(trainingSetDescriptor.problemList().size(), BASE_MAX_EVALUATIONS);
        int numberOfIndependentRuns = NUMBER_OF_INDEPENDENT_RUNS;

        EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

        MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem = new MetaOptimizationProblem<>(
                baseAlgorithm,
                trainingSet,
                referenceFrontFileNames,
                indicators,
                evaluationBudgetStrategy,
                numberOfIndependentRuns);

        // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the
        // specialized double builder
        EvolutionaryAlgorithm<DoubleSolution> nsgaii = new MetaNSGAIIBuilder(metaOptimizationProblem,
                new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()))
                .setMaxEvaluations(META_MAX_EVALUATIONS)
                .setNumberOfCores(NUMBER_OF_CORES)
                .build();

        // Step 4: Create observers for the meta-optimizer
        var config = MetaOptimizerConfig.builder()
                .metaOptimizerName("NSGA-II")
                .metaMaxEvaluations(META_MAX_EVALUATIONS)
                .metaPopulationSize(META_POPULATION_SIZE)
                .numberOfCores(NUMBER_OF_CORES)
                .baseLevelAlgorithmName("MOPSO")
                .baseLevelPopulationSize(BASE_POPULATION_SIZE)
                .evaluationBudgetStrategy("FixedEvaluations: " + maximumNumberOfEvaluations.get(0) + " per problem")
                .yamlParameterSpaceFile(yamlParameterSpaceFile)
                .build();

        var outputResults = new ConsolidatedOutputResults(
                metaOptimizationProblem,
                trainingSetDescriptor.name(),
                indicators,
                "RESULTS/MOPSO/" + trainingSetDescriptor.name(),
                config);

        var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

        var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
        var frontChartObserver = new FrontPlotObserver<DoubleSolution>(
                "MOPSO, " + trainingSetDescriptor.name(),
                indicators.get(0).name(),
                indicators.get(1).name(),
                trainingSetDescriptor.name(),
                PLOT_UPDATE_FREQUENCY);

        nsgaii.observable().register(evaluationObserver);
        nsgaii.observable().register(frontChartObserver);
        nsgaii.observable().register(writeExecutionDataToFilesObserver);

        // Step 5: Run the meta-optimizer
        nsgaii.run();

        // Step 6: Write results
        JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

        outputResults.updateEvaluations(META_MAX_EVALUATIONS);
        outputResults.writeResultsToFiles(nsgaii.result());

        System.exit(0);
    }
}
