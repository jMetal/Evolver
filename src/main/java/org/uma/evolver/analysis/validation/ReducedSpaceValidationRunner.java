package org.uma.evolver.analysis.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.builder.MetaAsyncNSGAIIBuilder;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.meta.strategy.EvaluationBudgetStrategy;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.RWA3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.ConsolidatedOutputResults;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.evolver.util.MetaOptimizerConfig;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Runs repeated meta-optimization validations using a reduced parameter space.
 *
 * <p>The output layout matches the current Python analysis loaders:
 *
 * <pre>
 * outputRoot/
 *   RE3D.referenceFronts.1000/
 *     run1/
 *       INDICATORS.csv
 *       CONFIGURATIONS.csv
 *     run2/
 *       ...
 * </pre>
 */
public final class ReducedSpaceValidationRunner {

  private static final int DEFAULT_META_MAX_EVALUATIONS = 3000;
  private static final int DEFAULT_META_POPULATION_SIZE = 50;
  private static final int DEFAULT_BASE_POPULATION_SIZE = 100;
  private static final int DEFAULT_BASE_INDEPENDENT_RUNS = 30;
  private static final int DEFAULT_WRITE_FREQUENCY = 100;
  private static final int DEFAULT_MAX_TRAINING_PROBLEMS = Integer.MAX_VALUE;

  private ReducedSpaceValidationRunner() {
    // Utility class
  }

  /**
   * Entry point for the validation runner.
   *
   * @param args command-line arguments
   * @throws IOException if writing outputs fails
   */
  public static void main(String[] args) throws IOException {
    ValidationConfig configuration = parseArgs(args);
    for (int runOffset = 0; runOffset < configuration.runs(); runOffset++) {
      runSingle(configuration, configuration.startRun() + runOffset);
    }
  }

  private static void runSingle(ValidationConfig configuration, int metaRun) throws IOException {
    TrainingSet<DoubleSolution> trainingSetDescriptor = createTrainingSet(configuration.family())
        .setReferenceFrontDirectory(configuration.referenceFrontDirectory())
        .setEvaluationsToOptimize(configuration.baseMaxEvaluations());

    List<Problem<DoubleSolution>> trainingSet = trainingSetDescriptor.problemList();
    List<String> referenceFrontFileNames = trainingSetDescriptor.referenceFronts();
    List<Integer> evaluationsToOptimize = trainingSetDescriptor.evaluationsToOptimize();
    if (configuration.maxTrainingProblems() < trainingSet.size()) {
      trainingSet = List.copyOf(trainingSet.subList(0, configuration.maxTrainingProblems()));
      referenceFrontFileNames =
          List.copyOf(referenceFrontFileNames.subList(0, configuration.maxTrainingProblems()));
      evaluationsToOptimize =
          List.copyOf(evaluationsToOptimize.subList(0, configuration.maxTrainingProblems()));
    }

    var indicators = List.of(new Epsilon(), new HypervolumeMinus());
    var parameterSpace =
        new YAMLParameterSpace(configuration.yamlParameterSpaceFile(), new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(configuration.basePopulationSize(), parameterSpace);
    EvaluationBudgetStrategy evaluationBudgetStrategy =
        new FixedEvaluationsStrategy(evaluationsToOptimize);

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            configuration.baseIndependentRuns());

    AsynchronousMultiThreadedNSGAII<DoubleSolution> metaOptimizer =
        new MetaAsyncNSGAIIBuilder(metaOptimizationProblem)
            .setNumberOfCores(configuration.numberOfCores())
            .setPopulationSize(configuration.metaPopulationSize())
            .setMaxEvaluations(configuration.metaMaxEvaluations())
            .build();

    Path sliceDirectory = configuration.outputRoot()
        .resolve(configuration.family() + "." + configuration.frontType() + "." + configuration.baseMaxEvaluations());
    Path runDirectory = sliceDirectory.resolve("run" + metaRun);
    ensureEmptyDirectory(runDirectory);

    MetaOptimizerConfig metadata =
        MetaOptimizerConfig.builder()
            .metaOptimizerName("AsyncNSGA-II")
            .metaMaxEvaluations(configuration.metaMaxEvaluations())
            .metaPopulationSize(configuration.metaPopulationSize())
            .numberOfCores(configuration.numberOfCores())
            .baseLevelAlgorithmName("NSGA-II")
            .baseLevelPopulationSize(configuration.basePopulationSize())
            .baseLevelMaxEvaluations(configuration.baseMaxEvaluations())
            .evaluationBudgetStrategy(evaluationBudgetStrategy.toString())
            .yamlParameterSpaceFile(configuration.yamlParameterSpaceFile())
            .build();

    var outputResults = new ConsolidatedOutputResults(
        metaOptimizationProblem,
        trainingSetDescriptor.name(),
        indicators,
        runDirectory.toString(),
        metadata);

    metaOptimizer.observable().register(
        new WriteExecutionDataToFilesObserver(configuration.writeFrequency(), outputResults));

    System.out.printf(
        "Running validation meta-run %d/%d for %s | %s | budget %d | YAML %s%n",
        metaRun,
        configuration.runs(),
        configuration.family(),
        configuration.frontType(),
        configuration.baseMaxEvaluations(),
        configuration.yamlParameterSpaceFile());

    metaOptimizer.run();
    outputResults.updateEvaluations(configuration.metaMaxEvaluations());
    outputResults.writeResultsToFiles(metaOptimizer.result());
  }

  private static TrainingSet<DoubleSolution> createTrainingSet(String family) {
    if ("RE3D".equalsIgnoreCase(family)) {
      return new RE3DTrainingSet();
    } else if ("RWA3D".equalsIgnoreCase(family)) {
      return new RWA3DTrainingSet();
    }
    throw new IllegalArgumentException("Unsupported family: " + family + " (expected RE3D or RWA3D)");
  }

  private static void ensureEmptyDirectory(Path runDirectory) throws IOException {
    Files.createDirectories(runDirectory);
    try (var stream = Files.list(runDirectory)) {
      if (stream.findAny().isPresent()) {
        throw new IllegalArgumentException("Output directory is not empty: " + runDirectory);
      }
    }
  }

  private static ValidationConfig parseArgs(String[] args) {
    if (args.length == 0 || args.length % 2 != 0) {
      printUsageAndExit();
    }

    Map<String, String> values = new HashMap<>();
    for (int index = 0; index < args.length; index += 2) {
      if (!args[index].startsWith("--")) {
        throw new IllegalArgumentException("Expected option starting with --, got: " + args[index]);
      }
      values.put(args[index], args[index + 1]);
    }

    String family = required(values, "--family");
    String frontType = required(values, "--front-type");
    String referenceFrontDirectory = required(values, "--reference-front-directory");
    int budget = parsePositiveInt(required(values, "--budget"), "--budget");
    int runs = parsePositiveInt(required(values, "--runs"), "--runs");
    int startRun =
        parsePositiveInt(values.getOrDefault("--start-run", "1"), "--start-run");
    int numberOfCores = parsePositiveInt(required(values, "--cores"), "--cores");
    String yamlParameterSpaceFile = required(values, "--yaml");
    Path outputRoot = Path.of(required(values, "--output-root"));

    int metaMaxEvaluations =
        parsePositiveInt(values.getOrDefault("--meta-evaluations", String.valueOf(DEFAULT_META_MAX_EVALUATIONS)),
            "--meta-evaluations");
    int metaPopulationSize =
        parsePositiveInt(values.getOrDefault("--meta-population-size", String.valueOf(DEFAULT_META_POPULATION_SIZE)),
            "--meta-population-size");
    int basePopulationSize =
        parsePositiveInt(values.getOrDefault("--base-population-size", String.valueOf(DEFAULT_BASE_POPULATION_SIZE)),
            "--base-population-size");
    int baseIndependentRuns = parsePositiveInt(
        values.getOrDefault("--base-independent-runs", String.valueOf(DEFAULT_BASE_INDEPENDENT_RUNS)),
        "--base-independent-runs");
    int writeFrequency =
        parsePositiveInt(values.getOrDefault("--write-frequency", String.valueOf(DEFAULT_WRITE_FREQUENCY)),
            "--write-frequency");
    int maxTrainingProblems =
        parsePositiveInt(
            values.getOrDefault(
                "--max-training-problems", String.valueOf(DEFAULT_MAX_TRAINING_PROBLEMS)),
            "--max-training-problems");

    return new ValidationConfig(
        family,
        frontType,
        referenceFrontDirectory,
        budget,
        runs,
        startRun,
        numberOfCores,
        yamlParameterSpaceFile,
        outputRoot,
        metaMaxEvaluations,
        metaPopulationSize,
        basePopulationSize,
        baseIndependentRuns,
        writeFrequency,
        maxTrainingProblems);
  }

  private static String required(Map<String, String> values, String key) {
    String value = values.get(key);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing required option: " + key);
    }
    return value;
  }

  private static int parsePositiveInt(String rawValue, String optionName) {
    int value = Integer.parseInt(rawValue);
    if (value <= 0) {
      throw new IllegalArgumentException(optionName + " must be positive: " + rawValue);
    }
    return value;
  }

  private static void printUsageAndExit() {
    String usage = String.join(
        System.lineSeparator(),
        "Usage: ReducedSpaceValidationRunner",
        "  --family <RE3D|RWA3D>",
        "  --front-type <referenceFronts|extremePointsFronts>",
        "  --reference-front-directory <path>",
        "  --budget <int>",
        "  --runs <int>",
        "  [--start-run <int>]",
        "  --cores <int>",
        "  --yaml <path>",
        "  --output-root <path>",
        "  [--meta-evaluations <int>]",
        "  [--meta-population-size <int>]",
        "  [--base-population-size <int>]",
        "  [--base-independent-runs <int>]",
        "  [--write-frequency <int>]",
        "  [--max-training-problems <int>]");
    throw new IllegalArgumentException(usage);
  }

  private record ValidationConfig(
      String family,
      String frontType,
      String referenceFrontDirectory,
      int baseMaxEvaluations,
      int runs,
      int startRun,
      int numberOfCores,
      String yamlParameterSpaceFile,
      Path outputRoot,
      int metaMaxEvaluations,
      int metaPopulationSize,
      int basePopulationSize,
      int baseIndependentRuns,
      int writeFrequency,
      int maxTrainingProblems) {
  }
}
