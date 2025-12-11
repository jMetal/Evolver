package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.mopso.BaseMOPSO;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.algorithm.meta.MetaNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.evolver.util.problemfamilyinfo.DTLZ3DProblemFamilyInfo;
import org.uma.evolver.util.problemfamilyinfo.ProblemFamilyInfo;
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

    public static void main(String[] args) throws IOException {
        String yamlParameterSpaceFile = "MOPSO.yaml";

        // Step 1: Select the target problem
        ProblemFamilyInfo<DoubleSolution> problemFamilyInfo = new DTLZ3DProblemFamilyInfo();

        List<Problem<DoubleSolution>> trainingSet = problemFamilyInfo.problemList();
        List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts();

        // Step 2: Set the parameters for the algorithm to be configured
        var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
        var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new MOPSOParameterFactory());

        var baseAlgorithm = new BaseMOPSO(100, parameterSpace);
        // Use small values for quick test
        var maximumNumberOfEvaluations = java.util.Collections.nCopies(problemFamilyInfo.problemList().size(), 250);
        int numberOfIndependentRuns = 1;

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
        int maxEvaluations = 100;
        int numberOfCores = 1;

        EvolutionaryAlgorithm<DoubleSolution> nsgaii = new MetaNSGAIIBuilder(metaOptimizationProblem,
                new NSGAIIDoubleParameterSpace())
                .setMaxEvaluations(maxEvaluations)
                .setNumberOfCores(numberOfCores)
                .build();

        // Step 4: Create observers for the meta-optimizer
        var config = MetaOptimizerConfig.builder()
                .metaOptimizerName("NSGA-II")
                .metaMaxEvaluations(maxEvaluations)
                .metaPopulationSize(100)
                .numberOfCores(numberOfCores)
                .baseLevelAlgorithmName("MOPSO")
                .baseLevelPopulationSize(100)
                .evaluationBudgetStrategy("FixedEvaluations: " + maximumNumberOfEvaluations.get(0) + " per problem")
                .yamlParameterSpaceFile(yamlParameterSpaceFile)
                .build();

        var outputResults = new ConsolidatedOutputResults(
                metaOptimizationProblem,
                "DTLZ",
                indicators,
                "RESULTS/MOPSO/DTLZ",
                config);

        var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(10, outputResults);

        var evaluationObserver = new EvaluationObserver(50);
        var frontChartObserver = new FrontPlotObserver<DoubleSolution>(
                "MOPSO, DTLZ",
                indicators.get(0).name(),
                indicators.get(1).name(),
                trainingSet.get(0).name(),
                10);

        nsgaii.observable().register(evaluationObserver);
        nsgaii.observable().register(frontChartObserver);
        nsgaii.observable().register(writeExecutionDataToFilesObserver);

        // Step 5: Run the meta-optimizer
        nsgaii.run();

        // Step 6: Write results
        JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

        outputResults.updateEvaluations(maxEvaluations);
        outputResults.writeResultsToFiles(nsgaii.result());

        System.exit(0);
    }
}
