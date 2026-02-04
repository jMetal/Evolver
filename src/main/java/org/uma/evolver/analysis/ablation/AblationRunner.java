package org.uma.evolver.analysis.ablation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;

/**
 * Executes an ablation analysis based on an {@link AblationConfiguration}.
 */
public class AblationRunner {

  private final AblationConfiguration configuration;

  /**
   * Creates a new runner.
   *
   * @param configuration the ablation configuration
   */
  public AblationRunner(AblationConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
  }

  /**
   * Runs the ablation analysis and writes CSV outputs.
   */
  public void run() {
    var reporter = createReporter();
    var trainingSet = ProblemSuiteFactory.createTrainingSet(configuration.problemSuite());

    if (configuration.maxEvaluations() > 0) {
      trainingSet.setEvaluationsToOptimize(configuration.maxEvaluations());
    }

    var parameterSpace =
        new YAMLParameterSpace(configuration.yamlParameterSpaceFile(), new DoubleParameterFactory());
    var algorithmTemplate = new DoubleNSGAII(configuration.populationSize(), parameterSpace);

    Path outputDir = configuration.outputDirectory();
    Path scratchDir = outputDir.resolve("tmp");

    var evaluator = new TrainingSetAblationEvaluator(
        trainingSet,
        algorithmTemplate,
        parameterSpace,
        configuration.indicatorDefinition(),
        configuration.numberOfRuns(),
        configuration.numberOfThreads(),
        configuration.validateConfigurations(),
        scratchDir);

    var analysis = new AblationAnalysis(evaluator, configuration.indicatorDefinition().maximize(), reporter);

    Instant start = Instant.now();
    reporter.reportProgress("Path", 0, 1, "Starting");

    AblationResult pathResult = analysis.performPathAblation(
        configuration.baselineConfiguration(),
        configuration.optimizedConfiguration());

    reporter.reportProgress("Path", 1, 1, "Complete");

    reporter.reportProgress("LOO", 0, 1, "Starting");

    AblationResult looResult = analysis.performLeaveOneOut(
        configuration.optimizedConfiguration(),
        configuration.baselineConfiguration());

    reporter.reportProgress("LOO", 1, 1, "Complete");

    writeResults(outputDir, pathResult, looResult);

    Duration elapsed = Duration.between(start, Instant.now());
    System.out.printf("Ablation finished in %.2f minutes.%n", elapsed.toMillis() / 60000.0);
  }

  private ProgressReporter createReporter() {
    ProgressReporter result;
    if (configuration.enableProgressReporting()) {
      result = new ConsoleProgressReporter();
    } else {
      result = new NoOpProgressReporter();
    }
    return result;
  }

  private void writeResults(Path outputDir, AblationResult pathResult, AblationResult looResult) {
    try {
      Files.createDirectories(outputDir);
      Path pathFile = outputDir.resolve(configuration.outputPrefix() + "_path.csv");
      Path looFile = outputDir.resolve(configuration.outputPrefix() + "_loo.csv");

      Files.writeString(pathFile, pathResult.toCSV());
      Files.writeString(looFile, looResult.toCSV());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write ablation results", e);
    }
  }
}
