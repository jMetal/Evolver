package org.uma.evolver.cli.training;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.builder.MetaAsyncNSGAIIBuilder;
import org.uma.evolver.meta.builder.MetaRandomSearchBuilder;
import org.uma.evolver.meta.builder.MetaSMPSOBuilder;
import org.uma.evolver.meta.builder.MetaSPEA2Builder;
import org.uma.evolver.meta.builder.RandomSearch;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
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
 * <p>None of the registered names carry a "Parallel" qualifier: every engine here evaluates via
 * {@code numberOfCores} (whether through {@link MultiThreadedEvaluation} or, for
 * {@code "AsyncNSGA-II"}, its own asynchronous evaluation), so singling one out as "Parallel"
 * would be misleading rather than distinguishing.
 *
 * <p>Registered engines have three different shapes, with no common jMetal supertype exposing
 * {@code run()}/{@code result()}/{@code observable()} (they are duck-typed, not a shared
 * interface), so {@link TrainingRunner} needs to know which one it got before it can register
 * observers on it. {@link #familyOf(String)} is the single source of truth for that:
 * <ul>
 *   <li>{@link Family#EVOLUTIONARY}: built via {@link #resolveFlat}, returns an
 *       {@link EvolutionaryAlgorithm}. {@code "NSGA-II"} is built directly on {@link DoubleNSGAII},
 *       a full {@code BaseLevelAlgorithm} with its own operator
 *       catalogue exposed via {@code operatorFlags}. {@code "SPEA2"} is built via
 *       {@link MetaSPEA2Builder}, which hardcodes its own operators (SBX crossover, polynomial
 *       mutation, strength ranking, KNN density estimator, tournament selection) and exposes only
 *       {@code offspringPopulationSize}/{@code mutationProbabilityFactor} as optional
 *       {@code operatorFlags}.
 *   <li>{@link Family#ASYNCHRONOUS}: built via {@link #resolveFlatAsync}, returns an
 *       {@link AsynchronousMultiThreadedNSGAII} (currently {@code "AsyncNSGA-II"}; it hardcodes
 *       its own selection/replacement, so only its crossover/mutation operators are configurable,
 *       via a much smaller parameter space containing just those two categorical parameters).
 *   <li>{@link Family#PARTICLE_SWARM}: built via {@link #resolveFlatPso}, returns a
 *       {@link ParticleSwarmOptimizationAlgorithm} (currently {@code "SMPSO"}; not generic, fixed
 *       to {@code DoubleSolution}). {@link MetaSMPSOBuilder} exposes no operator catalogue at all
 *       (swarm size/evaluations/cores only), so {@code operatorFlags} must be empty.
 *   <li>{@link Family#RANDOM_SEARCH}: built via {@link #resolveFlatRandomSearch}, returns a
 *       {@link RandomSearch} (currently {@code "RandomSearch"}; a fourth, distinct shape — no
 *       population, no operators, {@link MetaRandomSearchBuilder} exposes only
 *       evaluations/cores, so {@code operatorFlags} must be empty, same as SMPSO).
 * </ul>
 */
final class MetaAlgorithmRegistry {

  enum Family {
    EVOLUTIONARY,
    ASYNCHRONOUS,
    PARTICLE_SWARM,
    RANDOM_SEARCH
  }

  /**
   * An operator flag {@link #resolveFlat}/{@link #resolveFlatPso} accept outside the algorithm's
   * own {@code operatorParameterSpaceFile} (null for both here) — see {@link #buildSPEA2}, the
   * only registered algorithm with any.
   */
  record OperatorFlagDescriptor(String name, String type, boolean required) {}

  /**
   * @param name the meta-optimizer algorithm name, resolved via {@link #familyOf}
   * @param supportsTree whether {@link #validateTreeAlgorithm} accepts this name
   * @param operatorParameterSpaceFile the {@code ParameterSpace} YAML file backing this
   *     algorithm's operator catalogue (same format as a base-level algorithm's own
   *     {@code yamlParameterSpaceFile}), or null when the algorithm hardcodes its operators
   *     instead ({@code SPEA2}, {@code SMPSO})
   * @param hardcodedOperatorFlags the operator flags accepted despite there being no {@code
   *     operatorParameterSpaceFile} — empty unless {@code operatorParameterSpaceFile} is null and
   *     the algorithm still exposes something (only {@code SPEA2} today)
   */
  record MetaAlgorithmDescriptor(
      String name,
      Family family,
      boolean supportsTree,
      String operatorParameterSpaceFile,
      List<OperatorFlagDescriptor> hardcodedOperatorFlags) {}

  private static final List<MetaAlgorithmDescriptor> ALGORITHMS =
      List.of(
          new MetaAlgorithmDescriptor(
              "NSGA-II", Family.EVOLUTIONARY, true, "NSGAIIMetaDouble.yaml", List.of()),
          new MetaAlgorithmDescriptor(
              "SPEA2",
              Family.EVOLUTIONARY,
              false,
              null,
              List.of(
                  new OperatorFlagDescriptor("offspringPopulationSize", "int", false),
                  new OperatorFlagDescriptor("mutationProbabilityFactor", "double", false))),
          new MetaAlgorithmDescriptor(
              "AsyncNSGA-II",
              Family.ASYNCHRONOUS,
              false,
              "AsyncNSGAIIMetaDouble.yaml",
              List.of()),
          new MetaAlgorithmDescriptor(
              "SMPSO", Family.PARTICLE_SWARM, false, null, List.of()),
          new MetaAlgorithmDescriptor(
              "RandomSearch", Family.RANDOM_SEARCH, false, null, List.of()));

  /** Registered algorithms, for {@link DescribeMain}. All support the flat encoding. */
  static List<MetaAlgorithmDescriptor> registeredAlgorithms() {
    return ALGORITHMS;
  }

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String NSGAII_PARAMETER_SPACE_FILE = "NSGAIIMetaDouble.yaml";

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
  private static final String[] FIXED_NSGAII_FLAGS = {
    "--algorithmResult", "population",
    "--createInitialSolutions", "default",
    "--variation", "crossoverAndMutationVariation"
  };

  private MetaAlgorithmRegistry() {}

  static Family familyOf(String algorithmName) {
    return switch (algorithmName) {
      case "NSGA-II", "SPEA2" -> Family.EVOLUTIONARY;
      case "AsyncNSGA-II" -> Family.ASYNCHRONOUS;
      case "SMPSO" -> Family.PARTICLE_SWARM;
      case "RandomSearch" -> Family.RANDOM_SEARCH;
      default ->
          throw new JMetalException(
              "Unknown meta-optimizer algorithm: "
                  + algorithmName
                  + " for encoding flat. Supported: NSGA-II, SPEA2, AsyncNSGA-II, SMPSO,"
                  + " RandomSearch");
    };
  }

  static EvolutionaryAlgorithm<DoubleSolution> resolveFlat(
      String algorithmName,
      MetaOptimizationProblem<?> problem,
      FlatMetaSearchConfig config) {
    if (familyOf(algorithmName) != Family.EVOLUTIONARY) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not an EvolutionaryAlgorithm");
    }
    return "SPEA2".equals(algorithmName) ? buildSPEA2(problem, config) : buildNSGAII(problem, config);
  }

  static AsynchronousMultiThreadedNSGAII<DoubleSolution> resolveFlatAsync(
      String algorithmName,
      MetaOptimizationProblem<?> problem,
      FlatMetaSearchConfig config) {
    if (familyOf(algorithmName) != Family.ASYNCHRONOUS) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not an AsynchronousMultiThreadedNSGAII");
    }
    return buildAsyncNSGAII(problem, config);
  }

  static ParticleSwarmOptimizationAlgorithm resolveFlatPso(
      String algorithmName,
      MetaOptimizationProblem<?> problem,
      FlatMetaSearchConfig config) {
    if (familyOf(algorithmName) != Family.PARTICLE_SWARM) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not a ParticleSwarmOptimizationAlgorithm");
    }
    return buildSMPSO(problem, config);
  }

  static RandomSearch<DoubleSolution> resolveFlatRandomSearch(
      String algorithmName,
      MetaOptimizationProblem<?> problem,
      FlatMetaSearchConfig config) {
    if (familyOf(algorithmName) != Family.RANDOM_SEARCH) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not a RandomSearch");
    }
    return buildRandomSearch(problem, config);
  }

  private static EvolutionaryAlgorithm<DoubleSolution> buildNSGAII(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
    ParameterSpace parameterSpace =
        new YAMLParameterSpace(NSGAII_PARAMETER_SPACE_FILE, new DoubleParameterFactory());
    int populationSize =
        config.metaPopulationSize() == null ? DEFAULT_POPULATION_SIZE : config.metaPopulationSize();

    DoubleNSGAII metaNSGAII =
        new DoubleNSGAII(problem, populationSize, config.metaMaxEvaluations(), parameterSpace);
    metaNSGAII.parse(concat(FIXED_NSGAII_FLAGS, config.operatorFlags()));

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = metaNSGAII.build();
    nsgaii.evaluation(new MultiThreadedEvaluation<>(config.numberOfCores(), problem));
    return nsgaii;
  }

  private static AsynchronousMultiThreadedNSGAII<DoubleSolution> buildAsyncNSGAII(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
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

  private static EvolutionaryAlgorithm<DoubleSolution> buildSPEA2(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
    int populationSize =
        config.metaPopulationSize() == null ? DEFAULT_POPULATION_SIZE : config.metaPopulationSize();

    var builder =
        new MetaSPEA2Builder(problem)
            .setPopulationSize(populationSize)
            .setMaxEvaluations(config.metaMaxEvaluations())
            .setNumberOfCores(config.numberOfCores());
    optionalIntFlag(config.operatorFlags(), "--offspringPopulationSize")
        .ifPresent(builder::setOffspringPopulationSize);
    optionalDoubleFlag(config.operatorFlags(), "--mutationProbabilityFactor")
        .ifPresent(builder::setMutationProbabilityFactor);
    return builder.build();
  }

  private static ParticleSwarmOptimizationAlgorithm buildSMPSO(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
    requireNoOperatorFlags("SMPSO", config);
    int swarmSize =
        config.metaPopulationSize() == null ? DEFAULT_POPULATION_SIZE : config.metaPopulationSize();

    return new MetaSMPSOBuilder(problem)
        .setSwarmSize(swarmSize)
        .setMaxEvaluations(config.metaMaxEvaluations())
        .setNumberOfCores(config.numberOfCores())
        .build();
  }

  private static RandomSearch<DoubleSolution> buildRandomSearch(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
    requireNoOperatorFlags("RandomSearch", config);
    return new MetaRandomSearchBuilder<>(problem)
        .setMaxEvaluations(config.metaMaxEvaluations())
        .setNumberOfCores(config.numberOfCores())
        .build();
  }

  private static void requireNoOperatorFlags(String algorithmName, FlatMetaSearchConfig config) {
    if (!config.operatorFlags().isEmpty()) {
      throw new JMetalException(
          algorithmName
              + " exposes no operator catalogue; unexpected meta-optimizer configuration fields: "
              + config.operatorFlags());
    }
  }

  private static Optional<Integer> optionalIntFlag(List<String> flags, String flagName) {
    return optionalFlagValue(flags, flagName).map(Integer::parseInt);
  }

  private static Optional<Double> optionalDoubleFlag(List<String> flags, String flagName) {
    return optionalFlagValue(flags, flagName).map(Double::parseDouble);
  }

  private static Optional<String> optionalFlagValue(List<String> flags, String flagName) {
    int index = flags.indexOf(flagName);
    return index < 0 ? Optional.empty() : Optional.of(flags.get(index + 1));
  }

  /**
   * The tree encoding has no pluggable meta-optimizer builders yet (its pipeline is assembled
   * directly in {@link TrainingRunner#runTree}) — this only validates the request's declared
   * algorithm against the sole one that pipeline implements.
   */
  static void validateTreeAlgorithm(String algorithmName) {
    if (!"NSGA-II".equals(algorithmName)) {
      throw new JMetalException(
          "Unknown meta-optimizer algorithm: "
              + algorithmName
              + " for encoding tree. Supported: NSGA-II");
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
