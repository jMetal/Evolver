package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.operator.TreeMutation;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeOutputResults;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Asynchronous tree-encoded NSGA-II as meta-optimizer to configure {@link DoubleNSGAII} using the
 * RE3D benchmark problems as training set.
 *
 * <p>This is the tree-encoding equivalent of {@link AsyncNSGAIIOptimizingNSGAIIForBenchmarkRE3D}.
 * The meta-optimizer operates on derivation tree solutions using typed subtree crossover and tree
 * mutation instead of the flat [0,1]^n double encoding.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncTreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 3000;
  private static final int META_POPULATION_SIZE = 50;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;

  // Tree operator parameters
  private static final double CROSSOVER_PROBABILITY = 0.9;
  private static final double MUTATION_PROBABILITY = 1.0;
  private static final double MUTATION_DISTRIBUTION_INDEX = 20.0;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 100;
  private static final int WRITE_FREQUENCY = 100;
  private static final int PLOT_UPDATE_FREQUENCY = 100;

  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.err.println(
          "Usage: AsyncTreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D "
              + "<referenceFrontDirectory> <maximumNumberOfEvaluations> <numberOfCores> <resultsDirectory>");
      System.exit(1);
    }

    String referenceFrontDirectory = args[0];
    int baseMaxEvaluations = Integer.parseInt(args[1]);
    int numberOfCores = Integer.parseInt(args[2]);
    String resultsDirectory = args[3];

    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";

    // Step 1: Select the target problem
    TrainingSet<DoubleSolution> trainingSetDescriptor =
        new RE3DTrainingSet()
            .setReferenceFrontDirectory(referenceFrontDirectory)
            .setEvaluationsToOptimize(baseMaxEvaluations);

    List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
    List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new HypervolumeMinus());
    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, parameterSpace);
    var maximumNumberOfEvaluations = trainingSetDescriptor.evaluationsToOptimize();

    EvaluationBudgetStrategy evaluationBudgetStrategy =
        new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

    // Step 3: Create the tree-based meta-optimization problem
    var treeSolutionGenerator = new TreeSolutionGenerator(parameterSpace);

    TreeMetaOptimizationProblem<DoubleSolution> metaProblem =
        new TreeMetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS,
            treeSolutionGenerator);

    // Step 4: Set up the asynchronous meta-optimizer with tree operators
    var crossover = new SubtreeCrossover(CROSSOVER_PROBABILITY);
    var mutation = new TreeMutation(
        MUTATION_PROBABILITY, MUTATION_DISTRIBUTION_INDEX, treeSolutionGenerator);

    AsynchronousMultiThreadedNSGAII<DerivationTreeSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores,
            metaProblem,
            META_POPULATION_SIZE,
            crossover,
            mutation,
            new TerminationByEvaluations(META_MAX_EVALUATIONS));

    // Step 5: Create observers for the meta-optimizer
    String algorithmName = "AsyncTreeNSGA-II";
    String problemName = trainingSetDescriptor.name();

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(algorithmName)
            .metaMaxEvaluations(META_MAX_EVALUATIONS)
            .metaPopulationSize(META_POPULATION_SIZE)
            .numberOfCores(numberOfCores)
            .baseLevelAlgorithmName("NSGA-II")
            .baseLevelPopulationSize(BASE_POPULATION_SIZE)
            .baseLevelMaxEvaluations(maximumNumberOfEvaluations.get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(yamlParameterSpaceFile)
            .build();

    var outputResults = new TreeOutputResults(
        metaProblem, problemName, indicators, resultsDirectory, config, WRITE_FREQUENCY);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DerivationTreeSolution>(
            "AsyncTreeNSGA-II, " + trainingSetDescriptor.name(),
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSetDescriptor.name(),
            PLOT_UPDATE_FREQUENCY);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(outputResults);

    // Step 6: Run the meta-optimizer
    nsgaii.run();

    // Step 7: Write final results
    outputResults.writeFinalResults(nsgaii.result(), META_MAX_EVALUATIONS);

    System.exit(0);
  }
}
