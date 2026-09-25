package org.uma.evolver.cli.solving;

import java.util.List;
import java.util.Map;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.cli.ProblemRegistry;
import org.uma.evolver.cli.ProblemSpec;

/**
 * Describes a solve run: a configurable algorithm, with a given configuration, run on a problem
 * one or more times — see {@code docs/proposals/cli-solving.md}.
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
    String outputDirectory) {}
