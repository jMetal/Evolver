package org.uma.evolver.example.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GeneralizedSpread;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Code of the algorithm guide of NSGA-II (see {@code docs/algorithms/nsgaii.rst}).
 *
 * <p>Step 1 runs NSGA-II with its default configuration on ZDT1. Step 2 derives three variants from
 * that configuration by changing one parameter: the steady-state version (one offspring per
 * generation), and the versions that return an external archive instead of the population, bounded
 * with the crowding distance or unbounded. Step 3 runs the four of them on ZDT1, ZDT4 and DTLZ2 (three
 * objectives), {@value #INDEPENDENT_RUNS} times each, with a population of {@value #POPULATION_SIZE}
 * and {@value #MAX_EVALUATIONS} evaluations.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * guide includes; keep them when editing this class.
 */
public class NSGAIIGuide {

  static final String OUTPUT_DIRECTORY = "results/algorithm-guides/nsgaii";
  static final int INDEPENDENT_RUNS = 15;
  static final int MAX_EVALUATIONS = 25000;
  static final int NUMBER_OF_CORES = 16;
  static final int POPULATION_SIZE = 100;

  private NSGAIIGuide() {}

  public static void main(String[] args) throws IOException {
    runOnZDT1();
    compare(OUTPUT_DIRECTORY, INDEPENDENT_RUNS, 1.0, NUMBER_OF_CORES);
  }

  /** Step 1: NSGA-II with its default configuration on ZDT1. */
  static List<DoubleSolution> runOnZDT1() throws IOException {
    // [step-1-start]
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    var nsgaII =
        new DoubleNSGAII(
            new ZDT1(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    nsgaII.parse(configuration.split("\\s+"));

    Algorithm<List<DoubleSolution>> algorithm = nsgaII.build();
    algorithm.run();
    List<DoubleSolution> front = algorithm.result();
    // [step-1-end]
    System.out.println(algorithm.name() + " on ZDT1: " + front.size() + " solutions");
    return front;
  }

  /**
   * Step 2: the four configurations of the experiment, as tag and configuration string. Each variant
   * changes one parameter of the default configuration.
   */
  static Map<String, String> variants() throws IOException {
    // [step-2-start]
    String standard =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    String steadyState =
        standard.replace("--offspringPopulationSize 100", "--offspringPopulationSize 1");
    String crowdingDistanceArchive =
        standard.replace(
            "--algorithmResult population",
            "--algorithmResult externalArchive --populationSizeWithArchive 100"
                + " --archiveType crowdingDistanceArchive");
    String unboundedArchive =
        standard.replace(
            "--algorithmResult population",
            "--algorithmResult externalArchive --populationSizeWithArchive 100"
                + " --archiveType unboundedArchive");
    // [step-2-end]

    Map<String, String> variants = new LinkedHashMap<>();
    variants.put("NSGAII", standard);
    variants.put("NSGAIISteadyState", steadyState);
    variants.put("NSGAIICrowdingArchive", crowdingDistanceArchive);
    variants.put("NSGAIIUnboundedArchive", unboundedArchive);
    return variants;
  }

  /**
   * Step 3: the four configurations on the three problems. The evaluation budget is {@value
   * #MAX_EVALUATIONS} multiplied by {@code budgetFactor} (1.0 in the guide; smaller in the test).
   */
  static void compare(
      String outputDirectory, int independentRuns, double budgetFactor, int numberOfCores)
      throws IOException {
    // [step-3-start]
    List<ExperimentProblem<DoubleSolution>> problems =
        List.of(
            new ExperimentProblem<>(new ZDT1(), "ZDT1").setReferenceFront("ZDT1.csv"),
            new ExperimentProblem<>(new ZDT4(), "ZDT4").setReferenceFront("ZDT4.csv"),
            new ExperimentProblem<>(new DTLZ2(), "DTLZ2").setReferenceFront("DTLZ2.3D.csv"));

    int evaluations = (int) (budgetFactor * MAX_EVALUATIONS);
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> problem : problems) {
        for (Map.Entry<String, String> variant : variants().entrySet()) {
          algorithms.add(
              new ExperimentAlgorithm<>(
                  nsgaII(problem.getProblem(), evaluations, variant.getValue()),
                  variant.getKey(),
                  problem,
                  run));
        }
      }
    }

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("comparison")
            .setAlgorithmList(algorithms)
            .setProblemList(problems)
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(outputDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(
                List.of(
                    new Epsilon(),
                    new PISAHypervolume(),
                    new InvertedGenerationalDistancePlus(),
                    new GeneralizedSpread()))
            .setIndependentRuns(independentRuns)
            .setNumberOfCores(numberOfCores)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    // [step-3-end]
  }

  private static Algorithm<List<DoubleSolution>> nsgaII(
      Problem<DoubleSolution> problem, int maxEvaluations, String configuration) {
    var nsgaII =
        new DoubleNSGAII(
            problem,
            POPULATION_SIZE,
            maxEvaluations,
            new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    return nsgaII.parse(configuration.split("\\s+")).build();
  }
}
