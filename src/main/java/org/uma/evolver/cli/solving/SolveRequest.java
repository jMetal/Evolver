package org.uma.evolver.cli.solving;

import java.util.List;
import java.util.Map;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.cli.ProblemRegistry;
import org.uma.evolver.cli.ProblemSpec;

/**
 * Describes a solve run: a configurable algorithm, with a given configuration, run on a problem
 * one or more times.
 *
 * <p>The algorithm fields ({@code algorithmName}, {@code encoding}, {@code populationSize}, {@code
 * yamlParameterSpaceFile}, {@code extraConfig}) have the same names and meaning as in a training
 * run's base-level configuration, and are resolved by the same {@link BaseAlgorithmRegistry}; the
 * problem is a {@link ProblemSpec}, resolved by {@link ProblemRegistry}.
 *
 * @param algorithmName the algorithm name, e.g. {@code "NSGA-II"}
 * @param encoding the solution encoding, e.g. {@code "Double"}
 * @param populationSize the population size
 * @param yamlParameterSpaceFile the parameter space the configuration is parsed against
 * @param extraConfig algorithm-specific extra configuration (e.g. {@code
 *     weightVectorFilesDirectory} for MOEA/D); empty when not needed
 * @param configuration the configuration string ({@code --param value ...}); when the request
 *     gives a {@code configurationFile}, the first configuration of that file
 * @param configurationFile the file the configuration was read from, or null when the request gives
 *     the configuration string directly
 * @param problem the problem to solve
 * @param referenceFrontFileName the reference front of the problem, or null; required by {@code
 *     indicatorNames}
 * @param maxEvaluations the evaluation budget of each run
 * @param numberOfIndependentRuns the number of independent runs
 * @param seed the seed of the first run (run {@code i} uses {@code seed + i - 1}), or null to draw
 *     one at random
 * @param indicatorNames the quality indicators computed for each run; empty for none
 * @param statusFrequency every how many evaluations {@code status.yaml} is updated while a run is
 *     in progress, or null to update it only when a run ends. The more often, the more the run is
 *     slowed down
 * @param frontFrequency every how many evaluations the non-dominated front of the run in progress
 *     is written to {@code CURRENT_FRONT.csv} in the output directory (overwriting the previous
 *     one), or null to write no front while a run is in progress. Counted in the evaluations of
 *     each run; the more often, the slower the run
 * @param writePopulation whether that file holds the whole population instead of only its
 *     non-dominated solutions, each marked as dominated or not; requires {@code frontFrequency}
 * @param frontDelayMillis how long, in milliseconds, the run pauses after writing each front, so
 *     that a process that follows it can show every one instead of only the latest; null for no
 *     pause. It slows the run down by that much for each front; requires {@code frontFrequency}
 * @param outputDirectory where the results are written
 */
public record SolveRequest(
    String algorithmName,
    String encoding,
    int populationSize,
    String yamlParameterSpaceFile,
    Map<String, String> extraConfig,
    String configuration,
    String configurationFile,
    ProblemSpec problem,
    String referenceFrontFileName,
    int maxEvaluations,
    int numberOfIndependentRuns,
    Long seed,
    List<String> indicatorNames,
    Integer statusFrequency,
    Integer frontFrequency,
    boolean writePopulation,
    Integer frontDelayMillis,
    String outputDirectory) {

  /** A request that does not pause after writing a front. */
  public SolveRequest(
      String algorithmName,
      String encoding,
      int populationSize,
      String yamlParameterSpaceFile,
      Map<String, String> extraConfig,
      String configuration,
      String configurationFile,
      ProblemSpec problem,
      String referenceFrontFileName,
      int maxEvaluations,
      int numberOfIndependentRuns,
      Long seed,
      List<String> indicatorNames,
      Integer statusFrequency,
      Integer frontFrequency,
      boolean writePopulation,
      String outputDirectory) {
    this(
        algorithmName,
        encoding,
        populationSize,
        yamlParameterSpaceFile,
        extraConfig,
        configuration,
        configurationFile,
        problem,
        referenceFrontFileName,
        maxEvaluations,
        numberOfIndependentRuns,
        seed,
        indicatorNames,
        statusFrequency,
        frontFrequency,
        writePopulation,
        null,
        outputDirectory);
  }
}
