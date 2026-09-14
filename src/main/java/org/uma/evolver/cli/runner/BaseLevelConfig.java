package org.uma.evolver.cli.runner;

import java.util.List;
import java.util.Map;

/**
 * Describes what is being tuned and on what training set — the part of a {@link TrainingRequest}
 * that is independent of how the meta-optimizer searches the parameter space (see
 * {@link MetaSearchConfig}).
 *
 * <p>The training set is specified in exactly one of two ways, following the same
 * three-parallel-lists shape used throughout {@code org.uma.evolver.trainingset.TrainingSet}:
 * <ul>
 *   <li>{@code trainingSetName}: a named, multi-problem set resolved via
 *       {@link TrainingSetRegistry} (e.g. {@code "RE3D"}); {@code trainingProblemNames} must be
 *       null.
 *   <li>{@code trainingProblemNames} / {@code trainingReferenceFrontFileNames} /
 *       {@code trainingEvaluations}: three parallel lists (same size), each problem name resolved
 *       via {@link ProblemRegistry}; {@code trainingSetName} must be null.
 * </ul>
 */
public record BaseLevelConfig(
    String algorithmName,
    int populationSize,
    int numberOfIndependentRuns,
    String yamlParameterSpaceFile,
    Map<String, String> extraConfig,
    String trainingSetName,
    List<String> trainingProblemNames,
    List<String> trainingReferenceFrontFileNames,
    List<Integer> trainingEvaluations,
    List<String> indicatorNames,
    String outputDirectory) {}
