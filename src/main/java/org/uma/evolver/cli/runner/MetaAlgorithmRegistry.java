package org.uma.evolver.cli.runner;

import java.util.List;
import java.util.stream.Stream;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.builder.MetaAsyncNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builds the meta-optimizer for the flat [0,1]^n encoding, from an algorithm name and a
 * {@link FlatMetaSearchConfig}.
 *
 * <p>The meta-optimizer's own operators are configured the same way a base-level algorithm is: a
 * {@link ParameterSpace} declares what is configurable, and a fixed point within it is selected by
 * parsing {@link FlatMetaSearchConfig#operatorFlags()} — the same {@code ParameterSpace}/{@code
 * .parse(String[])} pattern already used for base-level algorithms, applied one level up. This
 * choice is fixed once per run, never evolved. Unlike a base-level algorithm's parameter space,
 * the meta-optimizer's own catalogue ({@code NSGAIIMetaDouble.yaml}/{@code
 * AsyncNSGAIIMetaDouble.yaml}) is not exposed as a request field: there is exactly one legal
 * catalogue per registered algorithm, so it is hardcoded here rather than repeated in every
 * meta-optimizer configuration file.
 *
 * <p>Registered engines have two different shapes, with no common jMetal supertype exposing
 * {@code run()}/{@code result()}/{@code observable()} (they are duck-typed, not a shared
 * interface), so {@link TrainingRunner} needs to know which one it got before it can register
 * observers on it. {@link #familyOf(String)} is the single source of truth for that:
 * <ul>
 *   <li>{@link Family#EVOLUTIONARY}: built via {@link #resolveFlat}, returns an
 *       {@link EvolutionaryAlgorithm} (currently {@code "ParallelNSGA-II"} — named for its
 *       multi-threaded evaluation, since every NSGA-II variant used as a meta-optimizer runs this
 *       way — built directly on {@link DoubleNSGAII}, a full {@code BaseLevelAlgorithm} with its
 *       own operator catalogue).
 *   <li>{@link Family#ASYNCHRONOUS}: built via {@link #resolveFlatAsync}, returns an
 *       {@link AsynchronousMultiThreadedNSGAII} (currently {@code "AsyncNSGA-II"}; it hardcodes
 *       its own selection/replacement, so only its crossover/mutation operators are configurable,
 *       via a much smaller parameter space containing just those two categorical parameters).
 * </ul>
 */
final class MetaAlgorithmRegistry {

  enum Family {
    EVOLUTIONARY,
    ASYNCHRONOUS
  }

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String PARALLEL_NSGAII_PARAMETER_SPACE_FILE = "NSGAIIMetaDouble.yaml";

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String ASYNC_NSGAII_PARAMETER_SPACE_FILE = "AsyncNSGAIIMetaDouble.yaml";

  private static final int DEFAULT_POPULATION_SIZE = 50;

  /**
   * {@code NSGAIIMetaDouble.yaml} declares {@code algorithmResult}, {@code
   * createInitialSolutions} and {@code variation} (all required by {@code BaseNSGAII.build()},
   * and {@code variation} is also what makes its {@code crossover}/{@code mutation} conditional
   * sub-parameters reachable) with only one legal value each — {@code .parse(String[])} still
   * requires their flags to be present, though, so they are fixed here rather than repeated in
   * every meta-optimizer configuration file (there is nothing for a user to choose between).
   */
  private static final String[] FIXED_PARALLEL_NSGAII_FLAGS = {
    "--algorithmResult", "population",
    "--createInitialSolutions", "default",
    "--variation", "crossoverAndMutationVariation"
  };

  private MetaAlgorithmRegistry() {}

  static Family familyOf(String algorithmName) {
    return switch (algorithmName) {
      case "ParallelNSGA-II" -> Family.EVOLUTIONARY;
      case "AsyncNSGA-II" -> Family.ASYNCHRONOUS;
      default ->
          throw new JMetalException(
              "Unknown meta-optimizer algorithm: "
                  + algorithmName
                  + " for encoding flat. Supported: ParallelNSGA-II, AsyncNSGA-II");
    };
  }

  static EvolutionaryAlgorithm<DoubleSolution> resolveFlat(
      String algorithmName,
      MetaOptimizationProblem<DoubleSolution> problem,
      FlatMetaSearchConfig config) {
    if (familyOf(algorithmName) != Family.EVOLUTIONARY) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not an EvolutionaryAlgorithm");
    }
    return buildNSGAII(problem, config);
  }

  static AsynchronousMultiThreadedNSGAII<DoubleSolution> resolveFlatAsync(
      String algorithmName,
      MetaOptimizationProblem<DoubleSolution> problem,
      FlatMetaSearchConfig config) {
    if (familyOf(algorithmName) != Family.ASYNCHRONOUS) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not an AsynchronousMultiThreadedNSGAII");
    }
    return buildAsyncNSGAII(problem, config);
  }

  private static EvolutionaryAlgorithm<DoubleSolution> buildNSGAII(
      MetaOptimizationProblem<DoubleSolution> problem, FlatMetaSearchConfig config) {
    ParameterSpace parameterSpace =
        new YAMLParameterSpace(PARALLEL_NSGAII_PARAMETER_SPACE_FILE, new DoubleParameterFactory());
    int populationSize =
        config.metaPopulationSize() == null ? DEFAULT_POPULATION_SIZE : config.metaPopulationSize();

    DoubleNSGAII metaNSGAII =
        new DoubleNSGAII(problem, populationSize, config.metaMaxEvaluations(), parameterSpace);
    metaNSGAII.parse(concat(FIXED_PARALLEL_NSGAII_FLAGS, config.operatorFlags()));

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = metaNSGAII.build();
    nsgaii.evaluation(new MultiThreadedEvaluation<>(config.numberOfCores(), problem));
    return nsgaii;
  }

  private static AsynchronousMultiThreadedNSGAII<DoubleSolution> buildAsyncNSGAII(
      MetaOptimizationProblem<DoubleSolution> problem, FlatMetaSearchConfig config) {
    ParameterSpace parameterSpace =
        new YAMLParameterSpace(ASYNC_NSGAII_PARAMETER_SPACE_FILE, new DoubleParameterFactory());
    applyFlags(parameterSpace, config.operatorFlags().toArray(new String[0]));

    var crossoverParameter = (DoubleCrossoverParameter) parameterSpace.get("crossover");
    var mutationParameter = (DoubleMutationParameter) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    int populationSize =
        config.metaPopulationSize() == null ? DEFAULT_POPULATION_SIZE : config.metaPopulationSize();

    return new MetaAsyncNSGAIIBuilder(problem)
        .setPopulationSize(populationSize)
        .setMaxEvaluations(config.metaMaxEvaluations())
        .setNumberOfCores(config.numberOfCores())
        .setCrossover(crossoverParameter.getCrossover())
        .setMutation(mutationParameter.getMutation())
        .build();
  }

  /**
   * The tree encoding has no pluggable meta-optimizer builders yet (its pipeline is assembled
   * directly in {@link TrainingRunner#runTree}) — this only validates the request's declared
   * algorithm against the sole one that pipeline implements.
   */
  static void validateTreeAlgorithm(String algorithmName) {
    if (!"ParallelNSGA-II".equals(algorithmName)) {
      throw new JMetalException(
          "Unknown meta-optimizer algorithm: "
              + algorithmName
              + " for encoding tree. Supported: ParallelNSGA-II");
    }
  }

  private static String[] concat(String[] fixedFlags, List<String> requestFlags) {
    return Stream.concat(Stream.of(fixedFlags), requestFlags.stream()).toArray(String[]::new);
  }

  private static void applyFlags(ParameterSpace parameterSpace, String[] flags) {
    for (Parameter<?> parameter : parameterSpace.topLevelParameters()) {
      parameter.parse(flags);
    }
  }
}
