package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.cli.ProblemRegistry;
import org.uma.evolver.cli.ProblemSpec;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.evolver.util.ConstrainedMultiobjectiveIndicator;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Code of tutorial E12, "Problems with constraints" (see {@code
 * docs/tutorials/problems_with_constraints.rst}).
 *
 * <p>It runs NSGA-II without and with a constraint-handling technique on DASCMOP2 and DASCMOP5,
 * two problems with constraints, and measures the runs with ICMOP; tunes NSGA-II, technique
 * included, for DASCMOP5, writing the whole population of the meta-optimizer to follow the
 * configurations without feasible solutions; and runs the tuned configuration against the default
 * one.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class ProblemsWithConstraintsTutorial {

  private static final int POPULATION_SIZE = 100;
  /** Evaluations of each run of NSGA-II outside the training, as in the literature budget. */
  private static final int EVALUATIONS = 20000;

  /** Independent runs of each configuration outside the training. */
  private static final int RUNS = 15;
  private static final String OUTPUT_DIRECTORY = "results/tutorial-problems-with-constraints";

  private ProblemsWithConstraintsTutorial() {}

  public static void main(String[] args) throws IOException {
    String withoutTechnique =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);
    String withTechnique =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleConstrainedDefault.txt")
            .getConfiguration(1);

    // [step-1-start]
    for (String problem : List.of("DASCMOP2", "DASCMOP5")) {
      System.out.println(
          problem
              + ", without a technique: "
              + runs("NSGAIIDouble.yaml", withoutTechnique, problem));
      System.out.println(
          problem
              + ", with cdp:            "
              + runs("NSGAIIDoubleConstrained.yaml", withTechnique, problem));
    }
    // [step-1-end]

    // [step-2-start]
    BaseLevelConfig baseLevel =
        BaseLevelConfigurationReader.loadFromYaml(
            """
            algorithmName: NSGA-II
            populationSize: 100
            numberOfIndependentRuns: 1
            yamlParameterSpaceFile: NSGAIIDoubleConstrained.yaml
            trainingProblemNames: [DASCMOP5]
            trainingReferenceFrontFileNames: [resources/referenceFronts/DASCMOP5.csv]
            trainingEvaluations: [10000]
            indicatorNames: [ConstrainedMultiobjectiveIndicator, Epsilon]
            """);
    MetaSearchConfig metaSearch =
        MetaOptimizerConfigurationReader.loadFromYaml(
            """
            algorithm: AsyncNSGA-II
            encoding: flat
            metaMaxEvaluations: 2000
            metaPopulationSize: 50
            numberOfCores: 14
            crossover: SBX
            mutation: polynomial
            crossoverProbability: 0.9
            crossoverRepairStrategy: bounds
            sbxDistributionIndex: 20.0
            mutationProbabilityFactor: 1.0
            mutationRepairStrategy: bounds
            polynomialMutationDistributionIndex: 20.0
            """);

    String trainingDirectory = OUTPUT_DIRECTORY + "/training";
    // A checkpoint every 10 evaluations, with the whole population of the meta-optimizer
    var request =
        new TrainingRequest(baseLevel, metaSearch, trainingDirectory, 10, 50, null, true);
    long start = System.currentTimeMillis();
    Path results = new TrainingRunner().run(request, Path.of(trainingDirectory, "status.yaml"));
    System.out.println(
        "Training finished in " + (System.currentTimeMillis() - start) / 1000 + " s");
    // [step-2-end]

    // [step-3-start]
    Map<Integer, int[]> feasibleConfigurations =
        feasibleConfigurations(results.resolve("POPULATION_INDICATORS.csv"));
    for (int evaluation : List.of(50, 100, 150, 200, 250, 300, 500, 1000, 2000)) {
      int[] counts = feasibleConfigurations.get(evaluation);
      System.out.printf(
          Locale.ROOT,
          "Evaluation %4d: %2d of %d configurations feasible%n",
          evaluation,
          counts[0],
          counts[1]);
    }
    String tuned = bestConfiguration(results.resolve("VAR_CONF.txt"));
    System.out.println("Tuned (lowest ICMOP): " + tuned);
    // [step-3-end]

    // [step-4-start]
    System.out.println(
        "Default (cdp): " + runs("NSGAIIDoubleConstrained.yaml", withTechnique, "DASCMOP5"));
    System.out.println(
        "Tuned:         " + runs("NSGAIIDoubleConstrained.yaml", tuned, "DASCMOP5"));
    // [step-4-end]

    System.exit(0);
  }

  /**
   * {@link #RUNS} runs of NSGA-II on a problem with {@link #EVALUATIONS} evaluations, with seeds 1
   * to {@link #RUNS}: how many of them end with feasible solutions, and their median ICMOP.
   */
  private static String runs(String space, String configuration, String problem)
      throws IOException {
    double[] values = new double[RUNS];
    int feasibleRuns = 0;
    for (int run = 0; run < RUNS; run++) {
      values[run] = icmop(space, configuration, problem, run + 1);
      feasibleRuns += values[run] <= 1.0 ? 1 : 0;
    }
    Arrays.sort(values);
    double median = (values[RUNS / 2] + values[(RUNS - 1) / 2]) / 2.0;
    return String.format(
        Locale.ROOT, "feasible in %2d of %d runs, median ICMOP %.3f", feasibleRuns, RUNS, median);
  }

  @SuppressWarnings("unchecked")
  private static double icmop(String space, String configuration, String problemName, int seed)
      throws IOException {
    JMetalRandom.getInstance().setSeed(seed);
    var problem = (Problem<DoubleSolution>) ProblemRegistry.resolve(new ProblemSpec(problemName));
    var nsgaii =
        new DoubleNSGAII(
            problem,
            POPULATION_SIZE,
            EVALUATIONS,
            new YAMLParameterSpace(space, new DoubleParameterFactory()));
    nsgaii.parse(configuration.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = nsgaii.build();
    algorithm.run();

    var nonDominated = new NonDominatedSolutionListArchive<DoubleSolution>();
    nonDominated.addAll(algorithm.result());
    double[][] referenceFront =
        VectorUtils.readVectors("resources/referenceFronts/" + problemName + ".csv", ",");
    return new ConstrainedMultiobjectiveIndicator(referenceFront).compute(nonDominated.solutions());
  }

  /**
   * For each checkpoint of {@code POPULATION_INDICATORS.csv}, the number of configurations of the
   * population that found feasible solutions (ICMOP at most 1: with one run on one problem, a
   * configuration without them has 6 or more) and the size of the population.
   */
  private static Map<Integer, int[]> feasibleConfigurations(Path populationIndicators)
      throws IOException {
    Map<Integer, int[]> counts = new TreeMap<>();
    List<String> lines = Files.readAllLines(populationIndicators);
    List<String> header = Arrays.asList(lines.get(0).split(","));
    int icmopColumn = header.indexOf("ICMOP");
    for (String line : lines.subList(1, lines.size())) {
      String[] values = line.split(",");
      int[] count = counts.computeIfAbsent(Integer.parseInt(values[0]), key -> new int[2]);
      count[0] += Double.parseDouble(values[icmopColumn]) <= 1.0 ? 1 : 0;
      count[1]++;
    }
    return counts;
  }

  /**
   * The configuration with the lowest ICMOP of the last checkpoint of a training's {@code
   * VAR_CONF.txt} file, the meta-optimizer's final front.
   */
  private static String bestConfiguration(Path varConfFile) throws IOException {
    Map<String, Double> lastFront = new LinkedHashMap<>();
    for (String line : Files.readAllLines(varConfFile)) {
      if (line.startsWith("# Evaluation")) {
        lastFront.clear();
      } else if (line.contains(" | ")) {
        String[] valuesAndConfiguration = line.split(" \\| ", 2);
        for (String value : valuesAndConfiguration[0].trim().split(" ")) {
          String[] nameAndValue = value.split("=");
          if (nameAndValue[0].equals("ICMOP")) {
            lastFront.put(valuesAndConfiguration[1].trim(), Double.parseDouble(nameAndValue[1]));
          }
        }
      }
    }
    List<Map.Entry<String, Double>> front = new ArrayList<>(lastFront.entrySet());
    front.sort(Map.Entry.comparingByValue());
    return front.get(0).getKey();
  }
}
