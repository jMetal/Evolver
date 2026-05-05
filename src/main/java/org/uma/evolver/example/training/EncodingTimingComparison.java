package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.operator.TreeMutation;
import org.uma.evolver.encoding.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.meta.builder.MetaAsyncNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Timing comparison between the flat double encoding (async NSGA-II) and the derivation tree
 * encoding (sync multi-threaded NSGA-II) on the RE3D training set.
 *
 * <p>Both runs use the same budget ({@link #META_MAX_EVALUATIONS} meta-evaluations), the same
 * training set, and the same number of parallel cores. Wall-clock time and throughput are reported
 * after each run.
 *
 * <p><b>Note on parallelism:</b> {@code AsynchronousMultiThreadedNSGAII} is typed to
 * {@code DoubleSolution} and cannot be used with the tree encoding. The tree run therefore uses
 * {@code EvolutionaryAlgorithm} with {@code MultiThreadedEvaluation}, which evaluates offspring in
 * parallel but synchronises at each generation boundary.
 *
 * <p>Usage:
 * <pre>
 *   EncodingTimingComparison &lt;referenceFrontDirectory&gt; &lt;baseMaxEvaluations&gt; &lt;numberOfCores&gt;
 * </pre>
 *
 * @author Antonio J. Nebro
 */
public class EncodingTimingComparison {

  // Profiling budget — keep low to measure per-evaluation overhead, not convergence
  private static final int META_MAX_EVALUATIONS = 200;
  private static final int META_POPULATION_SIZE = 50;
  private static final int META_OFFSPRING_SIZE = 50;
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;

  // Tree encoding operator parameters
  private static final double CROSSOVER_PROBABILITY = 0.9;
  private static final double MUTATION_PROBABILITY = 1.0;
  private static final double MUTATION_DISTRIBUTION_INDEX = 20.0;

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.err.println(
          "Usage: EncodingTimingComparison "
              + "<referenceFrontDirectory> <baseMaxEvaluations> <numberOfCores>");
      System.exit(1);
    }

    String referenceFrontDirectory = args[0];
    int baseMaxEvaluations = Integer.parseInt(args[1]);
    int numberOfCores = Integer.parseInt(args[2]);

    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";

    TrainingSet<DoubleSolution> trainingSet =
        new RE3DTrainingSet()
            .setReferenceFrontDirectory(referenceFrontDirectory)
            .setEvaluationsToOptimize(baseMaxEvaluations);

    List<Problem<DoubleSolution>> problems = trainingSet.problemList();
    List<String> referenceFronts = trainingSet.referenceFronts();
    List<Integer> evaluationsPerProblem = trainingSet.evaluationsToOptimize();
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    EvaluationBudgetStrategy budgetStrategy = new FixedEvaluationsStrategy(evaluationsPerProblem);

    System.out.println("=== Encoding Timing Comparison ===");
    System.out.printf(
        "Meta-evaluations: %d | Population: %d | Cores: %d%n",
        META_MAX_EVALUATIONS, META_POPULATION_SIZE, numberOfCores);
    System.out.printf(
        "Training set: %s | Base evals/problem: %d%n%n",
        trainingSet.name(), baseMaxEvaluations);
    System.out.println(
        "Note: flat encoding uses AsynchronousMultiThreadedNSGAII; tree encoding uses");
    System.out.println(
        "      EvolutionaryAlgorithm + MultiThreadedEvaluation (sync per generation).");
    System.out.println();

    // ------------------------------------------------------------------
    // Run 1: flat double encoding with async NSGA-II
    // ------------------------------------------------------------------
    var flatParamSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var flatBaseAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, flatParamSpace);

    MetaOptimizationProblem<DoubleSolution> flatProblem =
        new MetaOptimizationProblem<>(
            flatBaseAlgorithm,
            problems,
            referenceFronts,
            indicators,
            budgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS);

    AsynchronousMultiThreadedNSGAII<DoubleSolution> asyncNsgaii =
        new MetaAsyncNSGAIIBuilder(flatProblem)
            .setNumberOfCores(numberOfCores)
            .setPopulationSize(META_POPULATION_SIZE)
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .build();

    System.out.print("[1/2] Flat encoding (async NSGA-II) ... ");
    long flatStart = System.currentTimeMillis();
    asyncNsgaii.run();
    long flatMs = System.currentTimeMillis() - flatStart;
    System.out.printf("done in %d ms%n", flatMs);

    // ------------------------------------------------------------------
    // Run 2: derivation tree encoding with sync multi-threaded NSGA-II
    // ------------------------------------------------------------------
    var treeParamSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var treeBaseAlgorithm = new DoubleNSGAII(BASE_POPULATION_SIZE, treeParamSpace);
    var generator = new TreeSolutionGenerator(treeParamSpace);

    TreeMetaOptimizationProblem<DoubleSolution> treeProblem =
        new TreeMetaOptimizationProblem<>(
            treeBaseAlgorithm,
            problems,
            referenceFronts,
            indicators,
            budgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS,
            generator);

    var initialSolutions = new RandomSolutionsCreation<>(treeProblem, META_POPULATION_SIZE);
    var evaluation = new MultiThreadedEvaluation<DerivationTreeSolution>(numberOfCores, treeProblem);
    var termination = new TerminationByEvaluations(META_MAX_EVALUATIONS);
    var crossover = new SubtreeCrossover(CROSSOVER_PROBABILITY);
    var mutation = new TreeMutation(MUTATION_PROBABILITY, MUTATION_DISTRIBUTION_INDEX, generator);
    var variation = new CrossoverAndMutationVariation<>(META_OFFSPRING_SIZE, crossover, mutation);
    var ranking = new FastNonDominatedSortRanking<DerivationTreeSolution>();
    var densityEstimator = new CrowdingDistanceDensityEstimator<DerivationTreeSolution>();
    var replacement = new RankingAndDensityEstimatorReplacement<>(ranking, densityEstimator);
    var comparator = new MultiComparator<>(
        List.of(
            Comparator.comparing(ranking::getRank),
            Comparator.comparing(densityEstimator::value).reversed()));
    var selection = new NaryTournamentSelection<DerivationTreeSolution>(
        2, variation.matingPoolSize(), comparator);

    EvolutionaryAlgorithm<DerivationTreeSolution> treeNsgaii =
        new EvolutionaryAlgorithm<>(
            "TreeNSGAII",
            initialSolutions,
            evaluation,
            termination,
            selection,
            variation,
            replacement);

    System.out.print("[2/2] Tree encoding  (sync NSGA-II)  ... ");
    long treeStart = System.currentTimeMillis();
    treeNsgaii.run();
    long treeMs = System.currentTimeMillis() - treeStart;
    System.out.printf("done in %d ms%n", treeMs);

    // ------------------------------------------------------------------
    // Summary
    // ------------------------------------------------------------------
    double flatThroughput = META_MAX_EVALUATIONS * 1000.0 / flatMs;
    double treeThroughput = META_MAX_EVALUATIONS * 1000.0 / treeMs;
    double overheadPct = 100.0 * (treeMs - flatMs) / flatMs;

    System.out.println();
    System.out.println("=== Summary ===");
    System.out.printf("  Flat encoding (async NSGA-II): %7d ms   %.2f meta-evals/s%n",
        flatMs, flatThroughput);
    System.out.printf("  Tree encoding (sync  NSGA-II): %7d ms   %.2f meta-evals/s%n",
        treeMs, treeThroughput);
    System.out.printf("  Overhead: %+.1f%%  (tree/flat ratio = %.2fx)%n",
        overheadPct, (double) treeMs / flatMs);
    System.out.printf("%n  (JVM warmup favours run 2; re-run swapping order to cross-check)%n");
  }
}
