package org.uma.evolver.cli.training;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.uma.evolver.algorithm.agemoea.DoubleAGEMOEA;
import org.uma.evolver.algorithm.agemoea.TreeAGEMOEA;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.nsgaii.TreeNSGAII;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.builder.MetaAsyncNSGAIIBuilder;
import org.uma.evolver.meta.builder.MetaRandomSearchBuilder;
import org.uma.evolver.meta.builder.MetaSMPSOBuilder;
import org.uma.evolver.meta.builder.MetaSPEA2Builder;
import org.uma.evolver.meta.builder.RandomSearch;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.problem.TreeMetaOptimizationProblem;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.TreeParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builds the meta-optimizer from an algorithm name and a {@link FlatMetaSearchConfig} (flat
 * [0,1]^n encoding) or a {@link TreeMetaSearchConfig} (derivation tree encoding).
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
 * <p>Two settings are fixed for every population-based engine and never taken from a request:
 * the offspring population size always equals the meta population size (each generation's
 * offspring is evaluated in parallel as a whole), and the result is the final population, never an
 * external archive (meta-level fronts usually hold very few solutions). A request that tries to
 * set either one fails explicitly instead of being silently ignored.
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
 *       catalogue exposed via {@code operatorFlags}. {@code "AGE-MOEA"} is built the same way,
 *       directly on {@link DoubleAGEMOEA}, with the same operator catalogue plus its
 *       environmental selection variant ({@code agemoeaVariant}). {@code "SPEA2"} is built via
 *       {@link MetaSPEA2Builder}, which hardcodes its own operators (SBX crossover, polynomial
 *       mutation, strength ranking, KNN density estimator, tournament selection) and exposes only
 *       {@code mutationProbabilityFactor} as an optional {@code operatorFlags} entry.
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
 *
 * <p>For the tree encoding, {@link #resolveTree} builds {@code "NSGA-II"} and {@code "AGE-MOEA"}
 * on {@link TreeNSGAII}/{@link TreeAGEMOEA} ({@code NSGAIIMetaTree.yaml}/{@code
 * AGEMOEAMetaTree.yaml}, with subtree crossover and tree mutation), and {@link
 * #resolveTreeRandomSearch} builds {@code "RandomSearch"}. The remaining engines are flat-only:
 * {@code "SMPSO"} needs a {@code DoubleProblem}, and {@code "SPEA2"}/{@code "AsyncNSGA-II"} are
 * built with {@code DoubleSolution} operators.
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
              "AGE-MOEA", Family.EVOLUTIONARY, true, "AGEMOEAMetaDouble.yaml", List.of()),
          new MetaAlgorithmDescriptor(
              "SPEA2",
              Family.EVOLUTIONARY,
              false,
              null,
              List.of(new OperatorFlagDescriptor("mutationProbabilityFactor", "double", false))),
          new MetaAlgorithmDescriptor(
              "AsyncNSGA-II",
              Family.ASYNCHRONOUS,
              false,
              "AsyncNSGAIIMetaDouble.yaml",
              List.of()),
          new MetaAlgorithmDescriptor(
              "SMPSO", Family.PARTICLE_SWARM, false, null, List.of()),
          new MetaAlgorithmDescriptor(
              "RandomSearch", Family.RANDOM_SEARCH, true, null, List.of()));

  /**
   * Registered algorithms, for {@link DescribeMain}. All support the flat encoding; {@code
   * operatorParameterSpaceFile} names the flat one (the tree ones are {@code *MetaTree.yaml}).
   */
  static List<MetaAlgorithmDescriptor> registeredAlgorithms() {
    return ALGORITHMS;
  }

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String NSGAII_PARAMETER_SPACE_FILE = "NSGAIIMetaDouble.yaml";

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String AGEMOEA_PARAMETER_SPACE_FILE = "AGEMOEAMetaDouble.yaml";

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String NSGAII_TREE_PARAMETER_SPACE_FILE = "NSGAIIMetaTree.yaml";

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String AGEMOEA_TREE_PARAMETER_SPACE_FILE = "AGEMOEAMetaTree.yaml";

  /** Hardcoded, not user-facing — see class javadoc. */
  private static final String ASYNC_NSGAII_PARAMETER_SPACE_FILE = "AsyncNSGAIIMetaDouble.yaml";

  /**
   * Meta population size used when a meta-optimizer configuration omits {@code
   * metaPopulationSize}, for both encodings (also the default of every {@code meta.builder}).
   */
  static final int DEFAULT_POPULATION_SIZE = 50;

  /**
   * Flags fixed by the registry, not by the request, for a meta-optimizer built on a
   * {@code BaseLevelAlgorithm} parameter space. {@code NSGAIIMetaDouble.yaml}/{@code
   * AGEMOEAMetaDouble.yaml} declare {@code algorithmResult}, {@code createInitialSolutions} and
   * {@code variation} (all required by {@code BaseNSGAII.build()}/{@code BaseAGEMOEA.build()}, and {@code variation} is also what makes its {@code
   * crossover}/{@code mutation} conditional sub-parameters reachable) with only one legal value
   * each — {@code .parse(String[])} still requires their flags to be present, though, so they are
   * fixed here rather than repeated in every meta-optimizer configuration file (there is nothing
   * for a user to choose between). {@code offspringPopulationSize} is fixed to the population
   * size (see class javadoc).
   */
  private static String[] fixedFlags(int populationSize) {
    return new String[] {
      "--algorithmResult", "population",
      "--createInitialSolutions", "default",
      "--variation", "crossoverAndMutationVariation",
      "--offspringPopulationSize", String.valueOf(populationSize)
    };
  }

  /**
   * {@link #fixedFlags(int)} plus the tree encoding's only crossover ({@code subtree}) and
   * mutation ({@code tree}): there is nothing for a user to choose between.
   */
  private static String[] fixedTreeFlags(int populationSize) {
    return concat(
        fixedFlags(populationSize), List.of("--crossover", "subtree", "--mutation", "tree"));
  }

  private MetaAlgorithmRegistry() {}

  static Family familyOf(String algorithmName) {
    return switch (algorithmName) {
      case "NSGA-II", "AGE-MOEA", "SPEA2" -> Family.EVOLUTIONARY;
      case "AsyncNSGA-II" -> Family.ASYNCHRONOUS;
      case "SMPSO" -> Family.PARTICLE_SWARM;
      case "RandomSearch" -> Family.RANDOM_SEARCH;
      default ->
          throw new JMetalException(
              "Unknown meta-optimizer algorithm: "
                  + algorithmName
                  + " for encoding flat. Supported: NSGA-II, AGE-MOEA, SPEA2, AsyncNSGA-II, SMPSO,"
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
    return switch (algorithmName) {
      case "SPEA2" -> buildSPEA2(problem, config);
      case "AGE-MOEA" -> buildAGEMOEA(problem, config);
      default -> buildNSGAII(problem, config);
    };
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

    String[] fixedFlags = fixedFlags(populationSize);
    requireNoFixedFlags("NSGA-II", config.operatorFlags(), fixedFlags);

    DoubleNSGAII metaNSGAII =
        new DoubleNSGAII(problem, populationSize, config.metaMaxEvaluations(), parameterSpace);
    metaNSGAII.parse(concat(fixedFlags, config.operatorFlags()));

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = metaNSGAII.build();
    nsgaii.evaluation(new MultiThreadedEvaluation<>(config.numberOfCores(), problem));
    return nsgaii;
  }

  private static EvolutionaryAlgorithm<DoubleSolution> buildAGEMOEA(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
    ParameterSpace parameterSpace =
        new YAMLParameterSpace(AGEMOEA_PARAMETER_SPACE_FILE, new DoubleParameterFactory());
    int populationSize =
        config.metaPopulationSize() == null ? DEFAULT_POPULATION_SIZE : config.metaPopulationSize();

    String[] fixedFlags = fixedFlags(populationSize);
    requireNoFixedFlags("AGE-MOEA", config.operatorFlags(), fixedFlags);

    DoubleAGEMOEA metaAGEMOEA =
        new DoubleAGEMOEA(problem, populationSize, config.metaMaxEvaluations(), parameterSpace);
    metaAGEMOEA.parse(concat(fixedFlags, config.operatorFlags()));

    EvolutionaryAlgorithm<DoubleSolution> agemoea = metaAGEMOEA.build();
    agemoea.evaluation(new MultiThreadedEvaluation<>(config.numberOfCores(), problem));
    return agemoea;
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

    requireOnlyFlags("SPEA2", config.operatorFlags(), List.of("--mutationProbabilityFactor"));

    var builder =
        new MetaSPEA2Builder(problem)
            .setPopulationSize(populationSize)
            .setMaxEvaluations(config.metaMaxEvaluations())
            .setNumberOfCores(config.numberOfCores());
    optionalDoubleFlag(config.operatorFlags(), "--mutationProbabilityFactor")
        .ifPresent(builder::setMutationProbabilityFactor);
    return builder.build();
  }

  private static ParticleSwarmOptimizationAlgorithm buildSMPSO(
      MetaOptimizationProblem<?> problem, FlatMetaSearchConfig config) {
    requireNoOperatorFlags("SMPSO", config.operatorFlags());
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
    requireNoOperatorFlags("RandomSearch", config.operatorFlags());
    return new MetaRandomSearchBuilder<>(problem)
        .setMaxEvaluations(config.metaMaxEvaluations())
        .setNumberOfCores(config.numberOfCores())
        .build();
  }

  private static void requireNoOperatorFlags(String algorithmName, List<String> operatorFlags) {
    if (!operatorFlags.isEmpty()) {
      throw new JMetalException(
          algorithmName
              + " exposes no operator catalogue; unexpected meta-optimizer configuration fields: "
              + operatorFlags);
    }
  }

  /**
   * {@code .parse(String[])} takes the first occurrence of a flag, so a request flag repeating one
   * of {@code fixedFlags} would be silently ignored — fail instead, naming the offending flag.
   */
  private static void requireNoFixedFlags(
      String algorithmName, List<String> operatorFlags, String[] fixedFlags) {
    for (int i = 0; i < fixedFlags.length; i += 2) {
      if (operatorFlags.contains(fixedFlags[i])) {
        throw new JMetalException(
            algorithmName
                + " meta-optimizer: "
                + fixedFlags[i].substring(2)
                + " is fixed by the registry and cannot be set in a meta-optimizer configuration"
                + " (the offspring population size always equals metaPopulationSize, the"
                + " result is always the final population, and the tree encoding has a single"
                + " crossover and mutation)");
      }
    }
  }

  private static void requireOnlyFlags(
      String algorithmName, List<String> flags, List<String> allowedFlags) {
    for (int i = 0; i < flags.size(); i += 2) {
      if (!allowedFlags.contains(flags.get(i))) {
        throw new JMetalException(
            algorithmName
                + " meta-optimizer: unexpected meta-optimizer configuration field "
                + flags.get(i).substring(2)
                + ". Allowed: "
                + allowedFlags.stream().map(flag -> flag.substring(2)).toList());
      }
    }
  }

  private static Optional<Double> optionalDoubleFlag(List<String> flags, String flagName) {
    return optionalFlagValue(flags, flagName).map(Double::parseDouble);
  }

  private static Optional<String> optionalFlagValue(List<String> flags, String flagName) {
    int index = flags.indexOf(flagName);
    return index < 0 ? Optional.empty() : Optional.of(flags.get(index + 1));
  }

  /**
   * Fails unless {@code algorithmName} is registered with {@code supportsTree}, listing the
   * algorithms that are.
   */
  static void validateTreeAlgorithm(String algorithmName) {
    List<String> treeAlgorithms =
        ALGORITHMS.stream()
            .filter(MetaAlgorithmDescriptor::supportsTree)
            .map(MetaAlgorithmDescriptor::name)
            .toList();
    if (!treeAlgorithms.contains(algorithmName)) {
      throw new JMetalException(
          "Unknown meta-optimizer algorithm: "
              + algorithmName
              + " for encoding tree. Supported: "
              + String.join(", ", treeAlgorithms));
    }
  }

  static EvolutionaryAlgorithm<DerivationTreeSolution> resolveTree(
      String algorithmName, TreeMetaOptimizationProblem<?> problem, TreeMetaSearchConfig config) {
    validateTreeAlgorithm(algorithmName);
    if (familyOf(algorithmName) != Family.EVOLUTIONARY) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not an EvolutionaryAlgorithm");
    }

    boolean agemoea = "AGE-MOEA".equals(algorithmName);
    ParameterSpace parameterSpace =
        new YAMLParameterSpace(
            agemoea ? AGEMOEA_TREE_PARAMETER_SPACE_FILE : NSGAII_TREE_PARAMETER_SPACE_FILE,
            new TreeParameterFactory());
    int populationSize = config.metaPopulationSize();
    String[] fixedFlags = fixedTreeFlags(populationSize);
    requireNoFixedFlags(algorithmName, config.operatorFlags(), fixedFlags);

    String[] flags = concat(fixedFlags, config.operatorFlags());
    EvolutionaryAlgorithm<DerivationTreeSolution> algorithm;
    if (agemoea) {
      var treeAGEMOEA =
          new TreeAGEMOEA(problem, populationSize, config.metaMaxEvaluations(), parameterSpace);
      treeAGEMOEA.parse(flags);
      algorithm = treeAGEMOEA.build();
    } else {
      var treeNSGAII =
          new TreeNSGAII(problem, populationSize, config.metaMaxEvaluations(), parameterSpace);
      treeNSGAII.parse(flags);
      algorithm = treeNSGAII.build();
    }
    algorithm.evaluation(new MultiThreadedEvaluation<>(config.numberOfCores(), problem));
    return algorithm;
  }

  static RandomSearch<DerivationTreeSolution> resolveTreeRandomSearch(
      String algorithmName, TreeMetaOptimizationProblem<?> problem, TreeMetaSearchConfig config) {
    validateTreeAlgorithm(algorithmName);
    if (familyOf(algorithmName) != Family.RANDOM_SEARCH) {
      throw new JMetalException(
          "Meta-optimizer algorithm " + algorithmName + " is not a RandomSearch");
    }
    requireNoOperatorFlags(algorithmName, config.operatorFlags());
    return new MetaRandomSearchBuilder<>(problem)
        .setMaxEvaluations(config.metaMaxEvaluations())
        .setNumberOfCores(config.numberOfCores())
        .build();
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
