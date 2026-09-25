package org.uma.evolver.cli.solving;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.cli.BaseAlgorithmRegistry;
import org.uma.evolver.cli.IndicatorRegistry;
import org.uma.evolver.cli.ProblemRegistry;
import org.uma.evolver.cli.RunStatusWriter;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs a {@link SolveRequest}: builds the algorithm with its configuration, runs it {@code
 * numberOfIndependentRuns} times on the problem, one after another, and writes the results of each
 * run ({@code run-<i>/VAR.csv}, {@code run-<i>/FUN.csv}), the indicator values of all of them
 * ({@code INDICATORS.csv}) and the settings used ({@code METADATA.txt}).
 *
 * <p>Run {@code i} uses the seed {@code seed + i - 1}; when the request gives no seed, one is drawn
 * at random and recorded, so every run can be reproduced. The indicators are computed as in a
 * training run: on the non-dominated solutions of the result, with the front and the reference
 * front normalized to the bounds of the reference front.
 */
public class SolveRunner {

  /**
   * Runs the request, writing its progress to {@code statusFile}: the evaluations reported are
   * those of the runs already finished, out of {@code numberOfIndependentRuns * maxEvaluations}.
   *
   * @return the output directory
   */
  public Path run(SolveRequest request, Path statusFile) throws IOException {
    RunStatusWriter statusWriter = new RunStatusWriter(statusFile);
    int totalEvaluations = request.numberOfIndependentRuns() * request.maxEvaluations();
    int evaluationsDone = 0;
    try {
      BaseLevelAlgorithm<?> algorithm =
          BaseAlgorithmRegistry.resolve(
              request.algorithmName(),
              request.encoding(),
              request.populationSize(),
              BaseAlgorithmRegistry.resolveParameterSpace(
                  request.encoding(), request.yamlParameterSpaceFile()),
              request.extraConfig());
      Problem<?> problem = ProblemRegistry.resolve(request.problem());
      List<QualityIndicator> indicators =
          request.indicatorNames().stream().map(IndicatorRegistry::resolve).toList();
      double[][] referenceFront =
          request.referenceFrontFileName() == null
              ? null
              : VectorUtils.readVectors(request.referenceFrontFileName(), ",");
      String[] configuration = request.configuration().split("\\s+");
      long firstSeed = request.seed() == null ? new Random().nextInt(1_000_000) : request.seed();

      Path outputDirectory = Path.of(request.outputDirectory());
      Files.createDirectories(outputDirectory);
      writeMetadata(request, firstSeed, outputDirectory.resolve("METADATA.txt"));

      List<String> indicatorRows = new ArrayList<>();
      statusWriter.write(RunStatusWriter.State.RUNNING, 0, totalEvaluations);
      for (int run = 1; run <= request.numberOfIndependentRuns(); run++) {
        long seed = firstSeed + run - 1;
        JMetalRandom.getInstance().setSeed(seed);

        long startTime = System.currentTimeMillis();
        List<? extends Solution<?>> result =
            runOnce(algorithm, problem, request.maxEvaluations(), configuration);
        long computingTime = System.currentTimeMillis() - startTime;

        writeFront(result, outputDirectory.resolve("run-" + run));
        indicatorRows.add(
            indicatorRow(run, seed, computingTime, result, indicators, referenceFront));

        evaluationsDone += request.maxEvaluations();
        statusWriter.write(RunStatusWriter.State.RUNNING, evaluationsDone, totalEvaluations);
      }
      writeIndicators(indicators, indicatorRows, outputDirectory.resolve("INDICATORS.csv"));

      statusWriter.write(RunStatusWriter.State.FINISHED, totalEvaluations, totalEvaluations);
      return outputDirectory;
    } catch (RuntimeException | IOException e) {
      statusWriter.write(
          RunStatusWriter.State.FAILED, evaluationsDone, totalEvaluations, e.getMessage());
      throw e;
    }
  }

  /**
   * Casts the problem to the algorithm's own solution type, trusting that the request's {@code
   * encoding} matches the problem — the same trust model as the training runner: a mismatch
   * surfaces as a {@code ClassCastException} once a solution is evaluated.
   */
  @SuppressWarnings("unchecked")
  private static <S extends Solution<?>> List<S> runOnce(
      BaseLevelAlgorithm<S> algorithm,
      Problem<?> problem,
      int maxEvaluations,
      String[] configuration) {
    var instance =
        algorithm
            .createInstance((Problem<S>) problem, maxEvaluations)
            .parse(configuration)
            .build();
    instance.run();
    return instance.result();
  }

  private static void writeFront(List<? extends Solution<?>> result, Path runDirectory)
      throws IOException {
    Files.createDirectories(runDirectory);
    new SolutionListOutput(result)
        .setVarFileOutputContext(
            new DefaultFileOutputContext(runDirectory.resolve("VAR.csv").toString(), ","))
        .setFunFileOutputContext(
            new DefaultFileOutputContext(runDirectory.resolve("FUN.csv").toString(), ","))
        .print();
  }

  private static String indicatorRow(
      int run,
      long seed,
      long computingTime,
      List<? extends Solution<?>> result,
      List<QualityIndicator> indicators,
      double[][] referenceFront) {
    StringBuilder row = new StringBuilder(run + "," + seed + "," + computingTime);
    if (indicators.isEmpty()) {
      return row.toString();
    }
    double[][] front = nonDominatedFront(result);
    if (front[0].length != referenceFront[0].length) {
      throw new JMetalException(
          "Front dimension "
              + front[0].length
              + " does not match reference front dimension "
              + referenceFront[0].length);
    }
    double[] minimumValues = NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront);
    double[] maximumValues = NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront);
    double[][] normalizedFront = NormalizeUtils.normalize(front, minimumValues, maximumValues);
    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    for (QualityIndicator indicator : indicators) {
      QualityIndicator instance = indicator.newInstance();
      instance.referenceFront(normalizedReferenceFront);
      row.append(',').append(instance.compute(normalizedFront));
    }
    return row.toString();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static double[][] nonDominatedFront(List<? extends Solution<?>> result) {
    NonDominatedSolutionListArchive archive = new NonDominatedSolutionListArchive<>();
    archive.addAll((List) result);
    return getMatrixWithObjectiveValues(archive.solutions());
  }

  private static void writeIndicators(
      List<QualityIndicator> indicators, List<String> rows, Path indicatorsFile)
      throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(indicatorsFile.toFile()))) {
      StringBuilder header = new StringBuilder("Run,Seed,TimeMs");
      indicators.forEach(indicator -> header.append(',').append(indicator.name()));
      writer.println(header);
      rows.forEach(writer::println);
    }
  }

  private static void writeMetadata(SolveRequest request, long firstSeed, Path metadataFile)
      throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(metadataFile.toFile()))) {
      writer.println("# Solve run");
      writer.println("Date: " + LocalDateTime.now());
      writer.println("Algorithm: " + request.algorithmName());
      writer.println("Encoding: " + request.encoding());
      writer.println("Population size: " + request.populationSize());
      writer.println("Parameter space: " + request.yamlParameterSpaceFile());
      if (!request.extraConfig().isEmpty()) {
        writer.println("Extra configuration: " + request.extraConfig());
      }
      if (request.configurationFile() != null) {
        writer.println("Configuration file: " + request.configurationFile());
      }
      writer.println("Configuration: " + request.configuration());
      writer.println(
          "Problem: "
              + request.problem().className()
              + (request.problem().args().isEmpty() ? "" : " " + request.problem().args()));
      if (request.referenceFrontFileName() != null) {
        writer.println("Reference front: " + request.referenceFrontFileName());
      }
      writer.println("Maximum number of evaluations: " + request.maxEvaluations());
      writer.println("Independent runs: " + request.numberOfIndependentRuns());
      writer.println(
          String.format(
              Locale.ROOT,
              "Seeds: %d to %d%s",
              firstSeed,
              firstSeed + request.numberOfIndependentRuns() - 1,
              request.seed() == null ? " (drawn at random)" : ""));
      writer.println("Indicators: " + String.join(", ", request.indicatorNames()));
    }
  }
}
