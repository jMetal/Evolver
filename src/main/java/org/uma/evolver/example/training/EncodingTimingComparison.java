package org.uma.evolver.example.training;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.encoding.operator.SubtreeCrossover;
import org.uma.evolver.encoding.operator.TreeMutation;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
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
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Timing comparison between the flat double encoding and the derivation tree encoding, both using
 * {@link AsynchronousMultiThreadedNSGAII} as meta-optimizer on the RE3D training set.
 *
 * <p>Both runs use identical infrastructure (same async NSGA-II, same cores, same budget), so any
 * difference in wall-clock time is attributable solely to the encoding overhead — the extra work
 * done by tree construction, typed subtree crossover, and subtree mutation compared to the
 * plain SBX + polynomial mutation used on the flat encoding.
 *
 * <p>{@code AsynchronousMultiThreadedNSGAII} is generic over {@code Solution<?>} in jMetal, so it
 * can be instantiated directly with {@code DerivationTreeSolution} without any builder wrapper.
 *
 * <p>Usage:
 * <pre>
 *   EncodingTimingComparison &lt;referenceFrontDirectory&gt; &lt;baseMaxEvaluations&gt; &lt;numberOfCores&gt;
 * </pre>
 *
 * @author Antonio J. Nebro
 */
public class EncodingTimingComparison {

  private static final int META_MAX_EVALUATIONS = 200;
  private static final int META_POPULATION_SIZE = 50;
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
        "Training set: %s | Base evals/problem: %d%n",
        trainingSet.name(), baseMaxEvaluations);
    System.out.println("Meta-optimizer: AsynchronousMultiThreadedNSGAII (both runs)");
    System.out.println();

    // ------------------------------------------------------------------
    // Run 1: flat double encoding — async NSGA-II via MetaAsyncNSGAIIBuilder
    // ------------------------------------------------------------------
    var flatParamSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    MetaOptimizationProblem<DoubleSolution> flatProblem =
        new MetaOptimizationProblem<>(
            new DoubleNSGAII(BASE_POPULATION_SIZE, flatParamSpace),
            problems,
            referenceFronts,
            indicators,
            budgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS);

    AsynchronousMultiThreadedNSGAII<DoubleSolution> flatNsgaii =
        new MetaAsyncNSGAIIBuilder(flatProblem)
            .setNumberOfCores(numberOfCores)
            .setPopulationSize(META_POPULATION_SIZE)
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .build();

    System.out.print("[1/2] Flat encoding (async NSGA-II) ... ");
    long flatStart = System.currentTimeMillis();
    flatNsgaii.run();
    long flatMs = System.currentTimeMillis() - flatStart;
    System.out.printf("done in %d ms%n", flatMs);

    // ------------------------------------------------------------------
    // Run 2: derivation tree encoding — async NSGA-II instantiated directly
    // ------------------------------------------------------------------
    var treeParamSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var generator = new TreeSolutionGenerator(treeParamSpace);
    TreeMetaOptimizationProblem<DoubleSolution> treeProblem =
        new TreeMetaOptimizationProblem<>(
            new DoubleNSGAII(BASE_POPULATION_SIZE, treeParamSpace),
            problems,
            referenceFronts,
            indicators,
            budgetStrategy,
            NUMBER_OF_INDEPENDENT_RUNS,
            generator);

    AsynchronousMultiThreadedNSGAII<DerivationTreeSolution> treeNsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores,
            treeProblem,
            META_POPULATION_SIZE,
            new SubtreeCrossover(CROSSOVER_PROBABILITY),
            new TreeMutation(MUTATION_PROBABILITY, MUTATION_DISTRIBUTION_INDEX, generator),
            new TerminationByEvaluations(META_MAX_EVALUATIONS));

    System.out.print("[2/2] Tree encoding  (async NSGA-II) ... ");
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
    System.out.printf("  Tree encoding (async NSGA-II): %7d ms   %.2f meta-evals/s%n",
        treeMs, treeThroughput);
    System.out.printf("  Encoding overhead: %+.1f%%  (tree/flat = %.2fx)%n",
        overheadPct, (double) treeMs / flatMs);
    System.out.printf("%n  (JVM warmup favours run 2; re-run swapping order to cross-check)%n");
  }
}
