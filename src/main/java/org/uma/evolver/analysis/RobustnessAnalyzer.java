package org.uma.evolver.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;

/**
 * Analyzes the local robustness (sensitivity) of a configuration.
 *
 * <p>Perturbs the numerical parameters of a given configuration by a small amount (Gaussian noise)
 * and measures the variation in performance. This helps distinguish between stable "plateaus" and
 * unstable "peaks".
 *
 * @author Antonio J. Nebro
 */
public class RobustnessAnalyzer<S extends Solution<?>> {

  private final BaseLevelAlgorithm<S> algorithm;
  private final Problem<S> problem;
  private final ParameterSpace parameterSpace;
  private final List<QualityIndicator> indicators;
  private final double[][] referenceFront;
  private final int runsPerSample;
  private final int maxEvaluations;
  private final Random random = new Random();

  public RobustnessAnalyzer(
      BaseLevelAlgorithm<S> algorithm,
      Problem<S> problem,
      ParameterSpace parameterSpace,
      List<QualityIndicator> indicators,
      double[][] referenceFront,
      int maxEvaluations,
      int runsPerSample) {
    this.algorithm = algorithm;
    this.problem = problem;
    this.parameterSpace = parameterSpace;
    this.indicators = indicators;
    this.referenceFront = referenceFront; // Pre-normalized? Assuming input is raw and we normalize?
    // AblationAnalyzer takes raw list of ProblemWithReferenceFront.
    // Here let's take double[][] raw reference front.
    this.maxEvaluations = maxEvaluations;
    this.runsPerSample = runsPerSample;
  }

  /** Performs robustness analysis. */
  public List<Map<String, Object>> analyze(
      Map<String, String> centerConfig, int nSamples, double perturbationSigma) {

    List<Map<String, Object>> results = new ArrayList<>();

    // Normalize reference front once
    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);

    // 1. Evaluate Center (Baseline)
    System.out.println("Evaluating baseline configuration...");
    double[] baselinePerf = evaluate(centerConfig, normalizedReferenceFront);
    addResult(results, 0, "Baseline", centerConfig, baselinePerf);

    // 2. Evaluate Perturbed Samples
    System.out.println("Evaluating " + nSamples + " perturbed samples...");
    for (int i = 1; i <= nSamples; i++) {
      Map<String, String> perturbedConfig = perturb(centerConfig, perturbationSigma);
      double[] perf = evaluate(perturbedConfig, normalizedReferenceFront);
      addResult(results, i, "Perturbed", perturbedConfig, perf);
    }

    return results;
  }

  private Map<String, String> perturb(Map<String, String> config, double sigma) {
    Map<String, String> newConfig = new LinkedHashMap<>(config);

    for (Parameter<?> param : parameterSpace.parameters().values()) {
      String name = param.name();
      if (newConfig.containsKey(name) && isNumeric(newConfig.get(name))) {
        try {
          double val = Double.parseDouble(newConfig.get(name));
          double perturbedVal = val * (1.0 + (random.nextGaussian() * sigma));
          newConfig.put(name, String.valueOf(perturbedVal));

        } catch (NumberFormatException e) {
          // Ignore non-numeric
        }
      }
    }
    return newConfig;
  }

  private boolean isNumeric(String str) {
    return str.matches("-?\\d+(\\.\\d+)?");
  }

  private double[] evaluate(Map<String, String> config, double[][] normalizedReferenceFront) {
    String[] args = configToArgs(config);
    double[] avgIndicators = new double[indicators.size()];

    for (int r = 0; r < runsPerSample; r++) {
      // Create instance from base algorithm
      // BaseLevelAlgorithm needs a problem passed to createInstance?
      // Check AblationAnalyzer line 361:
      // algorithm.createInstance(problemData.problem(), maxEvaluations)
      // We stored algorithm but not the problem? BaseLevelAlgorithm holds the problem
      // internally usually?
      // No, BaseLevelAlgorithm acts as a factory.
      // I need to pass the problem to RobustnessAnalyzer or getter from
      // BaseLevelAlgorithm.
      // Let's assume BaseLevelAlgorithm has getProblem() or I need to update
      // constructor.
      // Simpler: pass problem in constructor. But I missed it in step.
      // Look at BaseLevelAlgorithm definition in previous file reads.
      // It seems createInstance needs problem.

      // Workaround: assume algorithm.getProblem() exists or retrieve it from
      // algorithm field if accessible.
      // Actually, let's just use algorithm.getDefaultProblem() if it exists or pass
      // it.
      // Wait, I can't check BaseLevelAlgorithm right now in this replace block.
      // I will assume I need to pass problem.
      // I will update constructor signature in a separate step if needed.
      // For now let's try `algorithm.getProblem()` which might not exist.
      // Actually, AblationAnalyzer passes problem explicitely.

      // Let's USE `algorithm.getProblem()` and if it fails I fix it.
      // BaseLevelAlgorithm stores `problem`.

      BaseLevelAlgorithm<S> instance = algorithm.createInstance(problem, maxEvaluations);
      instance.parse(args);

      Algorithm<List<S>> alg = instance.build();
      alg.run();

      List<S> front = alg.result();
      double[][] frontMatrix = solutionsToMatrix(front);
      double[][] normalizedFront = NormalizeUtils.normalize(frontMatrix);

      for (int i = 0; i < indicators.size(); i++) {
        indicators.get(i).referenceFront(normalizedReferenceFront);
        avgIndicators[i] += indicators.get(i).compute(normalizedFront);
      }
    }

    for (int i = 0; i < avgIndicators.length; i++) {
      avgIndicators[i] /= runsPerSample;
    }
    return avgIndicators;
  }

  private String[] configToArgs(Map<String, String> config) {
    List<String> args = new ArrayList<>();
    for (Map.Entry<String, String> entry : config.entrySet()) {
      args.add("--" + entry.getKey());
      args.add(entry.getValue());
    }
    return args.toArray(new String[0]);
  }

  private double[][] solutionsToMatrix(List<S> solutions) {
    if (solutions.isEmpty()) {
      return new double[0][0];
    }
    double[][] matrix = new double[solutions.size()][solutions.get(0).objectives().length];
    for (int i = 0; i < solutions.size(); i++) {
      matrix[i] = solutions.get(i).objectives().clone();
    }
    return matrix;
  }

  private void addResult(
      List<Map<String, Object>> results,
      int sampleId,
      String type,
      Map<String, String> config,
      double[] perf) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("SampleId", sampleId);
    row.put("Type", type);

    for (int i = 0; i < indicators.size(); i++) {
      row.put(indicators.get(i).name(), perf[i]);
    }
    results.add(row);
  }

  public void exportToCSV(List<Map<String, Object>> results, Path path) throws IOException {
    if (results.isEmpty()) return;

    try (PrintWriter writer = new PrintWriter(new FileWriter(path.toFile()))) {
      Map<String, Object> first = results.get(0);
      List<String> keys = new ArrayList<>(first.keySet());
      writer.println(String.join(",", keys));

      for (Map<String, Object> row : results) {
        List<String> values = new ArrayList<>();
        for (String key : keys) {
          values.add(String.valueOf(row.get(key)));
        }
        writer.println(String.join(",", values));
      }
    }
  }
}
