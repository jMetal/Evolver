package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.smsemoa.DoubleSMSEMOA;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link DoubleSMSEMOA} using problem
 * {@link ZDT4} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingSMSEMOAForProblemZDT4 {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 2000;
  private static final int META_POPULATION_SIZE = 100;
  private static final int META_OFFSPRING_POPULATION_SIZE = 100;
  private static final int META_TERMINATION_EVALUATIONS = 1000;
  private static final int NUMBER_OF_CORES = 8;
  private static final double CROSSOVER_PROBABILITY = 0.9;
  private static final double CROSSOVER_DISTRIBUTION_INDEX = 20.0;
  private static final double MUTATION_DISTRIBUTION_INDEX = 20.0;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;
  private static final int BASE_MAX_EVALUATIONS = 10000;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
  private static final int WRITE_FREQUENCY = 1;
  private static final int PLOT_UPDATE_FREQUENCY = 1;

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "SMSEMOADouble.yaml";

    // Step 1: Select the target problem
    List<Problem<DoubleSolution>> trainingSet = List.of(new ZDT4());
    List<String> referenceFrontFileNames = List.of("resources/referenceFronts/ZDT4.csv");

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleSMSEMOA(BASE_POPULATION_SIZE, parameterSpace);

    var maximumNumberOfEvaluations = List.of(BASE_MAX_EVALUATIONS);

    EvaluationBudgetStrategy evaluationBudgetStrategy =
        new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double
    // builder
    var crossover = new SBXCrossover(CROSSOVER_PROBABILITY, CROSSOVER_DISTRIBUTION_INDEX);

    double mutationProbability = 1.0 / metaOptimizationProblem.numberOfVariables();
    var mutation = new PolynomialMutation(mutationProbability, MUTATION_DISTRIBUTION_INDEX);

    Termination termination = new TerminationByEvaluations(META_TERMINATION_EVALUATIONS);

    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
        new NSGAIIBuilder<>(
                metaOptimizationProblem,
                META_POPULATION_SIZE,
                META_OFFSPRING_POPULATION_SIZE,
                crossover,
                mutation)
            .setTermination(termination)
            .setEvaluation(new MultiThreadedEvaluation<>(NUMBER_OF_CORES, metaOptimizationProblem))
            .build();

    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "SMSEMOA",
            metaOptimizationProblem,
            trainingSet.get(0).name(),
            indicators,
            "results/smsemoa/" + trainingSet.get(0).name());

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "SMSEMOA, " + trainingSet.get(0).name(),
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSet.get(0).name(),
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
