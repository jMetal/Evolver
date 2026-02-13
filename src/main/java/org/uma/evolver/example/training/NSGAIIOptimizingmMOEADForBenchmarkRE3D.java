package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.builder.MetaNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link DoubleNSGAII} using
 * the RE problems with three objectives as the training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingmMOEADForBenchmarkRE3D {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 2000;
  private static final int NUMBER_OF_CORES = 8;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
  private static final int WRITE_FREQUENCY = 1;
  private static final int PLOT_UPDATE_FREQUENCY = 1;

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "MOEADDouble.yaml";
    String weightVectorFilesDirectory = "resources/weightVectors" ;

    // Step 1: Select the target problem
    TrainingSet<DoubleSolution> trainingSetDescriptor = new RE3DTrainingSet();

    List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
    List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new InvertedGenerationalDistancePlus());
    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleMOEAD(BASE_POPULATION_SIZE, weightVectorFilesDirectory, parameterSpace);
    var maximumNumberOfEvaluations = trainingSetDescriptor.evaluationsToOptimize();
    int numberOfIndependentRuns = NUMBER_OF_INDEPENDENT_RUNS;

    EvaluationBudgetStrategy evaluationBudgetStrategy =
        new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            numberOfIndependentRuns);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double
    // builder
    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
        new MetaNSGAIIBuilder(metaOptimizationProblem, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()))
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .setNumberOfCores(NUMBER_OF_CORES)
            .build();

    // Step 4: Create observers for the meta-optimizer
    String algorithmName = "MOEAD";
    String problemName = trainingSetDescriptor.name();
    var outputResults =
        new OutputResults(
            algorithmName,
            metaOptimizationProblem,
            problemName,
            indicators,
            "RESULTS/MOEAD/" + problemName);

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "MOEAD, " + problemName,
            indicators.get(0).name(),
            indicators.get(1).name(),
            problemName,
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
