package org.uma.evolver.cli.runner;

import java.util.List;
import java.util.Map;

/**
 * Prototype request describing a single meta-optimization training run: NSGA-II tuning a
 * base-level algorithm over a training set.
 *
 * <p>Scope note: this is a study prototype for {@link TrainingRunner}, exercised against three
 * reference examples to avoid overfitting to a single case:
 * {@code NSGAIIOptimizingNSGAIIForProblemZDT4} (single problem), {@code
 * NSGAIIOptimizingNSGAIIForBenchmarkRE3D} (named multi-problem training set) and {@code
 * NSGAIIOptimizingMOEADForProblemZDT4} (a base-level algorithm other than NSGA-II). The
 * meta-optimizer itself is still fixed to NSGA-II, as it is in all three reference examples.
 *
 * <p>A training set is defined, throughout {@code org.uma.evolver.trainingset}, by three parallel
 * lists — the problems, their reference front files, and the number of evaluations to use for
 * each problem — plus a name (see {@link org.uma.evolver.trainingset.TrainingSet}). This request
 * specifies the training set in exactly one of two ways, following that same shape:
 * <ul>
 *   <li>{@code trainingSetName}: a named, multi-problem set resolved via
 *       {@link TrainingSetRegistry} (e.g. {@code "RE3D"}), which already provides the three
 *       lists; {@code trainingProblemNames} must be null.
 *   <li>{@code trainingProblemNames} / {@code trainingReferenceFrontFileNames} /
 *       {@code trainingEvaluations}: three parallel lists (same size), each problem name resolved
 *       via {@link ProblemRegistry}; {@code trainingSetName} must be null.
 * </ul>
 */
public record TrainingRequest(
    // Meta-optimizer configuration
    int metaMaxEvaluations,
    Integer metaPopulationSize,
    int numberOfCores,
    Double mutationProbabilityFactor,
    String metaYamlParameterSpaceFile,

    // Base-level algorithm configuration
    String baseLevelAlgorithmName,
    int baseLevelPopulationSize,
    int numberOfIndependentRuns,
    String baseLevelYamlParameterSpaceFile,
    Map<String, String> baseLevelExtraConfig,

    // Training set (exactly one of the two must be non-null)
    String trainingSetName,
    List<String> trainingProblemNames,
    List<String> trainingReferenceFrontFileNames,
    List<Integer> trainingEvaluations,

    // Output
    List<String> indicatorNames,
    String outputDirectory) {}
