package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.operator.TreeMutation;
import org.uma.evolver.encoding.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeOutputResults;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Class for running NSGA-II with tree encoding as meta-optimizer to configure {@link DoubleNSGAII}
 * using the ZDT problems as training set.
 *
 * <p>This example uses the derivation tree encoding instead of the flat [0,1]^n encoding. The
 * meta-optimizer operates directly on tree-structured solutions using typed subtree crossover
 * and point/subtree mutation.
 *
 * @author Antonio J. Nebro
 */
public class TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 2000;
  private static final int META_POPULATION_SIZE = 50;
  private static final int META_OFFSPRING_SIZE = 50;
  private static final int NUMBER_OF_CORES = 8;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;

  // Operator parameters
  private static final double CROSSOVER_PROBABILITY = 0.9;
  private static final double MUTATION_PROBABILITY = 1.0;
  private static final double MUTATION_DISTRIBUTION_INDEX = 20.0;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
  private static final int WRITE_FREQUENCY = 50;
  private static final int PLOT_UPDATE_FREQUENCY = 50;

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";

    // Step 1: Select the target problem
    TrainingSet<DoubleSolution> trainingSetDescriptor = new RE3DTrainingSet();
    List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
    List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
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

    // Step 4: Configure the meta-optimizer components with tree operators
    var initialSolutionsCreation =
        new RandomSolutionsCreation<>(metaProblem, META_POPULATION_SIZE);

    var evaluation =
        new MultiThreadedEvaluation<DerivationTreeSolution>(NUMBER_OF_CORES, metaProblem);

    var termination = new TerminationByEvaluations(META_MAX_EVALUATIONS);

    var crossover = new SubtreeCrossover(CROSSOVER_PROBABILITY);
    var mutation = new TreeMutation(
        MUTATION_PROBABILITY, MUTATION_DISTRIBUTION_INDEX, treeSolutionGenerator);
    var variation =
        new CrossoverAndMutationVariation<>(META_OFFSPRING_SIZE, crossover, mutation);

    var ranking = new FastNonDominatedSortRanking<DerivationTreeSolution>();
    var densityEstimator = new CrowdingDistanceDensityEstimator<DerivationTreeSolution>();
    var replacement = new RankingAndDensityEstimatorReplacement<>(ranking, densityEstimator);

    var rankingAndCrowdingComparator =
        new MultiComparator<>(List.of(
            Comparator.comparing(ranking::getRank),
            Comparator.comparing(densityEstimator::value).reversed()));

    var selection = new NaryTournamentSelection<DerivationTreeSolution>(
        2,
        variation.matingPoolSize(),
        rankingAndCrowdingComparator);

    // Step 5: Build the meta-optimizer
    EvolutionaryAlgorithm<DerivationTreeSolution> nsgaii =
        new EvolutionaryAlgorithm<>(
            "TreeNSGAII",
            initialSolutionsCreation,
            evaluation,
            termination,
            selection,
            variation,
            replacement);

    // Step 6: Create observers
    String algorithmName = "TreeNSGA-II";
    String problemName = trainingSetDescriptor.name();

    MetaOptimizerConfig config =
        MetaOptimizerConfig.builder()
            .metaOptimizerName(algorithmName)
            .metaMaxEvaluations(META_MAX_EVALUATIONS)
            .metaPopulationSize(META_POPULATION_SIZE)
            .numberOfCores(NUMBER_OF_CORES)
            .baseLevelAlgorithmName("NSGA-II")
            .baseLevelPopulationSize(BASE_POPULATION_SIZE)
            .baseLevelMaxEvaluations(maximumNumberOfEvaluations.get(0))
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(yamlParameterSpaceFile)
            .build();

    var outputResults = new TreeOutputResults(
        metaProblem, problemName, indicators,
        "experimentation/training/tree-nsgaii/" + problemName, config, WRITE_FREQUENCY);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DerivationTreeSolution>(
            "TreeNSGA-II, " + trainingSetDescriptor.name(),
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSetDescriptor.name(),
            PLOT_UPDATE_FREQUENCY);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(outputResults);

    // Step 7: Run the meta-optimizer
    nsgaii.run();

    // Step 8: Write final results
    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResults.writeFinalResults(nsgaii.result(), META_MAX_EVALUATIONS);

    System.exit(0);
  }
}
