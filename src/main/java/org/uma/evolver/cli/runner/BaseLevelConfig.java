package org.uma.evolver.cli.runner;

import java.util.List;
import java.util.Map;

/**
 * Describes what is being tuned and on what training set — the part of a {@link TrainingRequest}
 * that is independent of how the meta-optimizer searches the parameter space (see
 * {@link MetaSearchConfig}).
 *
 * <p>The training set is always given as three parallel lists of the same size — problem names
 * (resolved via {@link ProblemRegistry}), their reference front files, and the number of
 * evaluations to use for each problem — the same shape used throughout
 * {@code org.uma.evolver.trainingset.TrainingSet}. The CLI does not resolve training sets by
 * name: even a multi-problem set like RE3D is spelled out explicitly (see
 * {@code Re3dTrainingRunner}), so a request is always self-contained and never has to be
 * cross-referenced against {@code org.uma.evolver.trainingset}'s subclasses to know what it
 * actually runs.
 */
public record BaseLevelConfig(
    String algorithmName,
    int populationSize,
    int numberOfIndependentRuns,
    String yamlParameterSpaceFile,
    Map<String, String> extraConfig,
    List<String> trainingProblemNames,
    List<String> trainingReferenceFrontFileNames,
    List<Integer> trainingEvaluations,
    List<String> indicatorNames,
    String outputDirectory) {}
