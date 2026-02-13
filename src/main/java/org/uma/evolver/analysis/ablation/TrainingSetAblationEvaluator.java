package org.uma.evolver.analysis.ablation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.TrainingSetRunner;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;

/**
 * Evaluates configurations by running a base algorithm on a training set.
 */
public class TrainingSetAblationEvaluator implements AblationEvaluator {

  private final TrainingSet<DoubleSolution> trainingSet;
  private final BaseLevelAlgorithm<DoubleSolution> algorithmTemplate;
  private final ParameterSpace parameterSpace;
  private final IndicatorDefinition indicatorDefinition;
  private final int numberOfRuns;
  private final int numberOfThreads;
  private final boolean validateConfigurations;
  private final ConfigurationValidator configurationValidator;
  private final List<double[][]> referenceFronts;
  private final Path scratchOutputDir;

  /**
   * Creates a training-set-based evaluator.
   *
   * @param trainingSet training set to evaluate
   * @param algorithmTemplate base algorithm template
   * @param parameterSpace parameter space
   * @param indicatorDefinition indicator definition
   * @param numberOfRuns number of runs per configuration
   * @param numberOfThreads number of threads for evaluation
   * @param validateConfigurations whether to validate configurations
   * @param scratchOutputDir scratch directory for temporary outputs
   */
  public TrainingSetAblationEvaluator(
      TrainingSet<DoubleSolution> trainingSet,
      BaseLevelAlgorithm<DoubleSolution> algorithmTemplate,
      ParameterSpace parameterSpace,
      IndicatorDefinition indicatorDefinition,
      int numberOfRuns,
      int numberOfThreads,
      boolean validateConfigurations,
      Path scratchOutputDir) {

    if (trainingSet == null) {
      throw new IllegalArgumentException("Training set cannot be null");
    }
    if (algorithmTemplate == null) {
      throw new IllegalArgumentException("Algorithm template cannot be null");
    }
    if (parameterSpace == null) {
      throw new IllegalArgumentException("Parameter space cannot be null");
    }
    if (indicatorDefinition == null) {
      throw new IllegalArgumentException("Indicator definition cannot be null");
    }
    if (numberOfRuns <= 0) {
      throw new IllegalArgumentException("Number of runs must be positive: " + numberOfRuns);
    }
    if (numberOfThreads <= 0) {
      throw new IllegalArgumentException("Number of threads must be positive: " + numberOfThreads);
    }
    if (scratchOutputDir == null) {
      throw new IllegalArgumentException("Scratch output directory cannot be null");
    }

    this.trainingSet = trainingSet;
    this.algorithmTemplate = algorithmTemplate;
    this.parameterSpace = parameterSpace;
    this.indicatorDefinition = indicatorDefinition;
    this.numberOfRuns = numberOfRuns;
    this.numberOfThreads = numberOfThreads;
    this.validateConfigurations = validateConfigurations;
    this.configurationValidator = new ConfigurationValidator();
    this.referenceFronts = loadReferenceFronts(trainingSet);
    this.scratchOutputDir = scratchOutputDir;
  }

  @Override
  public double evaluate(Map<String, String> configuration) {
    if (validateConfigurations) {
      configurationValidator.validate(parameterSpace, configuration);
    }

    String[] args = ConfigurationParser.toArgs(configuration);
    double total = 0.0;

    for (int run = 0; run < numberOfRuns; run++) {
      var runner = new TrainingSetRunner.Builder<DoubleSolution>(trainingSet, algorithmTemplate, args)
          .outputDir(scratchOutputDir.toString())
          .numberOfThreads(numberOfThreads)
          .silent()
          .noOutput()
          .noIndicators()
          .build();

      Map<String, List<DoubleSolution>> results = runner.run();
      double runScore = computeMeanIndicator(results);
      total += runScore;
    }

    return total / numberOfRuns;
  }

  private List<double[][]> loadReferenceFronts(TrainingSet<DoubleSolution> set) {
    List<double[][]> result = new ArrayList<>();
    for (String referenceFront : set.referenceFronts()) {
      try {
        result.add(VectorUtils.readVectors(referenceFront, ","));
      } catch (IOException e) {
        throw new IllegalStateException("Failed to load reference front: " + referenceFront, e);
      }
    }
    return result;
  }

  private double computeMeanIndicator(Map<String, List<DoubleSolution>> results) {
    double sum = 0.0;
    int count = 0;

    var problems = trainingSet.problemList();
    for (int i = 0; i < problems.size(); i++) {
      String problemName = problems.get(i).getClass().getSimpleName();
      List<DoubleSolution> solutionList = results.get(problemName);
      if (solutionList == null || solutionList.isEmpty()) {
        throw new IllegalStateException("No results for problem: " + problemName);
      }
      double[][] front = SolutionListUtils.getMatrixWithObjectiveValues(solutionList);
      double[][] referenceFront = referenceFronts.get(i);

      QualityIndicator indicator = indicatorDefinition.factory().get();
      indicator.referenceFront(referenceFront);
      double value = indicator.compute(front);
      sum += value;
      count++;
    }

    if (count == 0) {
      throw new IllegalStateException("No indicator values computed");
    }

    return sum / count;
  }
}
