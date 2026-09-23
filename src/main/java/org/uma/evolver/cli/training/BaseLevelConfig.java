package org.uma.evolver.cli.training;

import java.util.List;
import java.util.Map;

/**
 * Describes what is being tuned and on what training set — the part of a {@link TrainingRequest}
 * that is independent of how the meta-optimizer searches the parameter space (see
 * {@link MetaSearchConfig}) and of where a particular run writes its results (see
 * {@link TrainingRequest#outputDirectory()}) — everything here is meant to be reused, unchanged,
 * across many training requests, loaded by name via {@link BaseLevelConfigurationReader}.
 *
 * <p>The training set is always given as three parallel lists of the same size — problems
 * (resolved via {@link ProblemRegistry}, each either a curated short name or a fully-qualified
 * class name, see {@link ProblemSpec}), their reference front files, and the number of
 * evaluations to use for each problem — the same shape used throughout
 * {@code org.uma.evolver.trainingset.TrainingSet}. The CLI does not resolve training sets by
 * name: even a multi-problem set like RE3D is spelled out explicitly (see
 * {@code cli.training.generators.Re3dBaseLevelConfigurationGenerator}), so a request is always
 * self-contained and never has to be cross-referenced against
 * {@code org.uma.evolver.trainingset}'s subclasses to know what it actually runs.
 *
 * <p>{@code encoding} is the jMetal solution encoding the base-level algorithm is built for (e.g.
 * {@code "Double"}, {@code "Permutation"}), resolved together with {@code algorithmName} by
 * {@link BaseAlgorithmRegistry}.
 */
public record BaseLevelConfig(
    String algorithmName,
    String encoding,
    int populationSize,
    int numberOfIndependentRuns,
    String yamlParameterSpaceFile,
    Map<String, String> extraConfig,
    List<ProblemSpec> trainingProblemNames,
    List<String> trainingReferenceFrontFileNames,
    List<Integer> trainingEvaluations,
    List<String> indicatorNames) {}
