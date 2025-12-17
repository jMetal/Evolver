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
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running SMPSO as meta-optimizer to configure {@link DoubleNSGAII} using
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
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml" ;

    // Step 1: Select the target problem
    List<Problem<DoubleSolution>> trainingSet = List.of(new RE31());
    List<String> referenceFrontFileNames = List.of("resources/referenceFronts/RE31.csv");

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var configurableAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, parameterSpace);

    var maximumNumberOfEvaluations = List.of(BASE_MAX_EVALUATIONS);

    EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            configurableAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS);

    // Step 3: Set up and configure the meta-optimizer (SMPSO) using the specialized builder
    ParticleSwarmOptimizationAlgorithm smpso =
        new MetaSMPSOBuilder(metaOptimizationProblem)
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .setNumberOfCores(NUMBER_OF_CORES)
            .build();

    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "NSGA-II",
            metaOptimizationProblem,
            trainingSet.get(0).name(),
            indicators,
            "results/smpso/nsgaii/" + trainingSet.get(0).name());

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
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
    JMetalLogger.logger.info(() -> "Total computing time: " + smpso.totalComputingTime());

    outputResults.updateEvaluations(META_MAX_EVALUATIONS);
    outputResults.writeResultsToFiles(smpso.result());

    System.exit(0);
  }
}
