package org.uma.evolver.util;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.util.trainingset.TrainingSet;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * Utility class for running a configured algorithm on all problems in a
 * TrainingSet.
 *
 * <p>
 * This class executes the algorithm on each problem, saves results to CSV
 * files, and optionally
 * computes quality indicators. Results are organized in a directory structure
 * suitable for
 * visualization with Python scripts.
 *
 * <p>
 * Output structure:
 *
 * <pre>
 * outputDir/
 *   problem1/
 *     FUN.csv
 *     VAR.csv
 *   problem2/
 *     FUN.csv
 *     VAR.csv
 *   ...
 *   manifest.csv
 *   indicators.csv
 * </pre>
 *
 * @param <S> the solution type
 */
public class TrainingSetRunner<S extends Solution<?>> {

  private final TrainingSet<S> trainingSet;
  private final BaseLevelAlgorithm<S> algorithmTemplate;
  private final String[] configuration;
  private final String outputDir;
  private final int numberOfThreads;
  private final List<QualityIndicator> indicators;

  private final boolean silent;

  private TrainingSetRunner(Builder<S> builder) {
    this.trainingSet = builder.trainingSet;
    this.algorithmTemplate = builder.algorithmTemplate;
    this.configuration = builder.configuration;
    this.outputDir = builder.outputDir;
    this.numberOfThreads = builder.numberOfThreads;
    this.indicators = builder.indicators;
    this.silent = builder.silent;
    this.outputDisabled = builder.outputDisabled;
  }

  private final boolean outputDisabled;

  /**
   * Runs the configured algorithm on all problems in the training set.
   *
   * @return map of problem names to their results (solution lists)
   */
  public Map<String, List<S>> run() {
    Map<String, List<S>> results = new LinkedHashMap<>();
    List<Problem<S>> problems = trainingSet.problemList();
    List<String> referenceFronts = trainingSet.referenceFronts();
    List<Integer> evaluations = trainingSet.evaluationsToOptimize();

    // Create output directory
    try {
      Files.createDirectories(Path.of(outputDir));
    } catch (IOException e) {
      throw new RuntimeException("Failed to create output directory: " + outputDir, e);
    }

    if (!silent) {
      System.out.println("Running " + trainingSet.name() + " training set (" + problems.size() + " problems)");
      System.out.println("Output directory: " + outputDir);
      System.out.println("Configuration: " + String.join(" ", configuration));
      System.out.println();
    }

    if (numberOfThreads == 1) {
      // Sequential execution
      for (int i = 0; i < problems.size(); i++) {
        Problem<S> problem = problems.get(i);
        String problemName = problem.getClass().getSimpleName();
        int maxEvaluations = evaluations.get(i);
        String referenceFront = referenceFronts.get(i);

        List<S> result = runSingleProblem(problem, problemName, maxEvaluations, referenceFront, i + 1, problems.size());
        results.put(problemName, result);
      }
    } else {
      // Parallel execution
      ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
      Map<String, List<S>> synchronizedResults = java.util.Collections.synchronizedMap(results);

      for (int i = 0; i < problems.size(); i++) {
        final int index = i;
        Problem<S> problem = problems.get(i);
        String problemName = problem.getClass().getSimpleName();
        int maxEvaluations = evaluations.get(i);
        String referenceFront = referenceFronts.get(i);

        executor.submit(() -> {
          List<S> result = runSingleProblem(problem, problemName, maxEvaluations, referenceFront, index + 1,
              problems.size());
          synchronizedResults.put(problemName, result);
        });
      }

      executor.shutdown();
      try {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Execution interrupted", e);
      }
    }

    // Write manifest and indicators
    if (!outputDisabled) {
      writeManifest(results);
      if (!indicators.isEmpty()) {
        writeIndicators(results);
      }
    }

    if (!silent) {
      System.out.println("\nAll problems completed. Results saved to: " + outputDir);
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  private List<S> runSingleProblem(Problem<S> problem, String problemName,
      int maxEvaluations, String referenceFront,
      int current, int total) {
    if (!silent) {
      System.out.printf("[%d/%d] Running %s (max evaluations: %d)...%n",
          current, total, problemName, maxEvaluations);
    }

    long startTime = System.currentTimeMillis();

    // Create algorithm instance for this problem
    BaseLevelAlgorithm<S> algorithm = algorithmTemplate.createInstance(problem, maxEvaluations);
    algorithm.parse(configuration);
    var builtAlgorithm = algorithm.build();

    // Run the algorithm
    builtAlgorithm.run();
    List<S> result = builtAlgorithm.result();

    long elapsed = System.currentTimeMillis() - startTime;

    // Save results
    if (!outputDisabled) {
      String problemDir = outputDir + "/" + problemName;
      try {
        Files.createDirectories(Path.of(problemDir));
        new SolutionListOutput(result)
            .setVarFileOutputContext(new DefaultFileOutputContext(problemDir + "/VAR.csv", ","))
            .setFunFileOutputContext(new DefaultFileOutputContext(problemDir + "/FUN.csv", ","))
            .print();
      } catch (IOException e) {
        if (!silent) {
          System.err.println("Warning: Failed to save results for " + problemName + ": " + e.getMessage());
        }
      }
    }

    // Compute indicators if reference front exists
    if (!indicators.isEmpty() && referenceFront != null) {
      try {
        double[][] reference = VectorUtils.readVectors(referenceFront, ",");
        double[][] front = SolutionListUtils.getMatrixWithObjectiveValues(result);

        StringBuilder indicatorValues = new StringBuilder();
        for (QualityIndicator indicator : indicators) {
          indicator.referenceFront(reference);
          double value = indicator.compute(front);
          indicatorValues.append(String.format("  %s: %.6f", indicator.name(), value));
        }
        if (!silent) {
          System.out.printf("  Completed in %.2fs.%s%n", elapsed / 1000.0, indicatorValues);
        }
      } catch (IOException e) {
        if (!silent) {
          System.out.printf("  Completed in %.2fs. (indicators unavailable)%n", elapsed / 1000.0);
        }
      }
    } else {
      if (!silent) {
        System.out.printf("  Completed in %.2fs.%n", elapsed / 1000.0);
      }
    }

    return result;
  }

  private void writeManifest(Map<String, List<S>> results) {
    String manifestPath = outputDir + "/manifest.csv";
    try (FileWriter writer = new FileWriter(manifestPath, StandardCharsets.UTF_8)) {
      writer.write("problem,reference_front,solutions\n");

      List<String> referenceFronts = trainingSet.referenceFronts();
      List<Problem<S>> problems = trainingSet.problemList();

      for (int i = 0; i < problems.size(); i++) {
        String problemName = problems.get(i).getClass().getSimpleName();
        String refFront = referenceFronts.get(i);
        int solutionCount = results.getOrDefault(problemName, List.of()).size();
        writer.write(String.format("%s,%s,%d%n", problemName, refFront, solutionCount));
      }
    } catch (IOException e) {
      System.err.println("Warning: Failed to write manifest: " + e.getMessage());
    }
  }

  private void writeIndicators(Map<String, List<S>> results) {
    String indicatorsPath = outputDir + "/indicators.csv";
    try (FileWriter writer = new FileWriter(indicatorsPath, StandardCharsets.UTF_8)) {
      // Header
      writer.write("problem");
      for (QualityIndicator indicator : indicators) {
        writer.write("," + indicator.name());
      }
      writer.write("\n");

      // Data
      List<String> referenceFronts = trainingSet.referenceFronts();
      List<Problem<S>> problems = trainingSet.problemList();

      for (int i = 0; i < problems.size(); i++) {
        String problemName = problems.get(i).getClass().getSimpleName();
        List<S> result = results.get(problemName);

        if (result == null || result.isEmpty())
          continue;

        writer.write(problemName);

        try {
          double[][] reference = VectorUtils.readVectors(referenceFronts.get(i), ",");
          double[][] front = SolutionListUtils.getMatrixWithObjectiveValues(result);

          for (QualityIndicator indicator : indicators) {
            indicator.referenceFront(reference);
            double value = indicator.compute(front);
            writer.write(String.format(",%.6f", value));
          }
        } catch (IOException e) {
          for (int j = 0; j < indicators.size(); j++) {
            writer.write(",NA");
          }
        }
        writer.write("\n");
      }
    } catch (IOException e) {
      System.err.println("Warning: Failed to write indicators: " + e.getMessage());
    }
  }

  /**
   * Builder for TrainingSetRunner.
   *
   * @param <S> the solution type
   */
  public static class Builder<S extends Solution<?>> {
    private final TrainingSet<S> trainingSet;
    private final BaseLevelAlgorithm<S> algorithmTemplate;
    private final String[] configuration;
    private String outputDir = "results/fronts";
    private int numberOfThreads = 1;
    private List<QualityIndicator> indicators = List.of(new Epsilon(), new NormalizedHypervolume());

    private boolean silent = false;
    private boolean outputDisabled = false;

    public Builder(TrainingSet<S> trainingSet, BaseLevelAlgorithm<S> algorithmTemplate, String[] configuration) {
      this.trainingSet = trainingSet;
      this.algorithmTemplate = algorithmTemplate;
      this.configuration = configuration;
    }

    public Builder<S> noOutput() {
      this.outputDisabled = true;
      return this;
    }

    public Builder<S> outputDir(String outputDir) {
      this.outputDir = outputDir;
      return this;
    }

    public Builder<S> numberOfThreads(int numberOfThreads) {
      this.numberOfThreads = numberOfThreads;
      return this;
    }

    public Builder<S> indicators(List<QualityIndicator> indicators) {
      this.indicators = indicators;
      return this;
    }

    public Builder<S> noIndicators() {
      this.indicators = List.of();
      return this;
    }

    /**
     * Suppresses standard output logging.
     * Useful when running inside other analysis loops (e.g., Ablation).
     */
    public Builder<S> silent() {
      this.silent = true;
      return this;
    }

    public TrainingSetRunner<S> build() {
      return new TrainingSetRunner<>(this);
    }
  }
}
