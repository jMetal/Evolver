package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.nsgaii.BinaryNSGAII;
import org.uma.evolver.algorithm.nsgaii.PermutationNSGAII;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateReferenceParetoFront;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAC100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAD100TSP;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAE100TSP;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT5;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Validation studies of tutorial E10 (binary and permutation encodings): the standard NSGA-II
 * against NSGA-II with a configuration tuned by {@code tutorial-encodings-binary-request.yaml} and by
 * {@code tutorial-encodings-permutation-request.yaml}, {@value #INDEPENDENT_RUNS} independent runs each.
 *
 * <ul>
 *   <li>Binary: ZDT5, the problem of the training, with {@value #BINARY_EVALUATIONS} evaluations;
 *       EP, HV and IGD+ against its exact reference front ({@code ZDT5.csv}).
 *   <li>Permutation: the bi-objective TSP instances KroAB100 and KroAC100 (seen during training)
 *       and KroAD100 and KroAE100 (not seen), with {@value #PERMUTATION_EVALUATIONS} evaluations.
 *       The instances have no reference front, and the two extreme points of {@code
 *       resources/referenceFrontsTSP/} used in the training cannot tell good fronts apart: on
 *       three instances their lower bounds are far above the routes found, so the fronts dominate
 *       the whole box and the hypervolume saturates. The study builds its own reference front instead, the
 *       non-dominated points of all the runs of both algorithms ({@code GenerateReferenceParetoFront}
 *       of jMetal), and computes EP, HV and IGD+ against it (tutorial E9).
 * </ul>
 *
 * <p>The tuned configurations are bundled in {@code tunedConfigurations/}; pass two files as
 * arguments (binary, permutation) to validate your own. Run it from the root of the repository.
 */
public class EncodingsTutorial {

  static final String BINARY_CONFIGURATION_FILE = "tunedConfigurations/NSGAIIBinaryZDT5.txt";
  static final String PERMUTATION_CONFIGURATION_FILE =
      "tunedConfigurations/NSGAIIPermutationKroTSP.txt";
  static final String OUTPUT_DIRECTORY = "results/tutorial-encodings";
  static final int INDEPENDENT_RUNS = 25;
  static final int BINARY_EVALUATIONS = 25000;
  static final int PERMUTATION_EVALUATIONS = 125000;
  static final int NUMBER_OF_CORES = 16;

  private static final int POPULATION_SIZE = 100;

  public static void main(String[] args) throws IOException {
    String binaryFile = args.length > 1 ? args[0] : BINARY_CONFIGURATION_FILE;
    String permutationFile = args.length > 1 ? args[1] : PERMUTATION_CONFIGURATION_FILE;
    runBinary(
        new ConfigurationFileReader(binaryFile).getConfiguration(1),
        OUTPUT_DIRECTORY,
        INDEPENDENT_RUNS,
        BINARY_EVALUATIONS,
        NUMBER_OF_CORES);
    runPermutation(
        new ConfigurationFileReader(permutationFile).getConfiguration(1),
        OUTPUT_DIRECTORY,
        INDEPENDENT_RUNS,
        PERMUTATION_EVALUATIONS,
        NUMBER_OF_CORES);
  }

  static void runBinary(
      String tunedConfiguration,
      String outputDirectory,
      int independentRuns,
      int maxEvaluations,
      int numberOfCores)
      throws IOException {
    // [binary-start]
    List<ExperimentProblem<BinarySolution>> problems =
        List.of(new ExperimentProblem<BinarySolution>(new ZDT5()).setReferenceFront("ZDT5.csv"));

    List<ExperimentAlgorithm<BinarySolution, List<BinarySolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<BinarySolution> problem : problems) {
        Problem<BinarySolution> p = problem.getProblem();
        algorithms.add(
            algorithm(
                new BinaryNSGAII(p, POPULATION_SIZE, maxEvaluations, binarySpace()),
                defaultConfiguration("NSGAIIBinaryDefault.txt"),
                "NSGAII",
                problem,
                run));
        algorithms.add(
            algorithm(
                new BinaryNSGAII(p, POPULATION_SIZE, maxEvaluations, binarySpace()),
                tunedConfiguration,
                "NSGAIIZDT5",
                problem,
                run));
      }
    }

    run(
        "binary",
        problems,
        algorithms,
        "resources/referenceFronts",
        List.of(new Epsilon(), new PISAHypervolume(), new InvertedGenerationalDistancePlus()),
        outputDirectory,
        independentRuns,
        numberOfCores,
        false);
    // [binary-end]
  }

  static void runPermutation(
      String tunedConfiguration,
      String outputDirectory,
      int independentRuns,
      int maxEvaluations,
      int numberOfCores)
      throws IOException {
    // [permutation-start]
    List<ExperimentProblem<PermutationSolution<Integer>>> problems = new ArrayList<>();
    // Seen during training
    problems.add(tsp(new KroAB100TSP()));
    problems.add(tsp(new KroAC100TSP()));
    // Not seen during training
    problems.add(tsp(new KroAD100TSP()));
    problems.add(tsp(new KroAE100TSP()));

    List<ExperimentAlgorithm<PermutationSolution<Integer>, List<PermutationSolution<Integer>>>>
        algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<PermutationSolution<Integer>> problem : problems) {
        Problem<PermutationSolution<Integer>> p = problem.getProblem();
        algorithms.add(
            algorithm(
                new PermutationNSGAII(p, POPULATION_SIZE, maxEvaluations, permutationSpace()),
                defaultConfiguration("NSGAIIPermutationDefault.txt"),
                "NSGAII",
                problem,
                run));
        algorithms.add(
            algorithm(
                new PermutationNSGAII(p, POPULATION_SIZE, maxEvaluations, permutationSpace()),
                tunedConfiguration,
                "NSGAIIKroTSP",
                problem,
                run));
      }
    }

    run(
        "permutation",
        problems,
        algorithms,
        outputDirectory + "/permutation/referenceFronts",
        List.of(new Epsilon(), new PISAHypervolume(), new InvertedGenerationalDistancePlus()),
        outputDirectory,
        independentRuns,
        numberOfCores,
        true);
    // [permutation-end]
  }

  private static ExperimentProblem<PermutationSolution<Integer>> tsp(
      Problem<PermutationSolution<Integer>> problem) {
    return new ExperimentProblem<>(problem).setReferenceFront(problem.name() + ".csv");
  }

  private static <S extends Solution<?>> void run(
      String name,
      List<ExperimentProblem<S>> problems,
      List<ExperimentAlgorithm<S, List<S>>> algorithms,
      String referenceFrontDirectory,
      List<QualityIndicator> indicators,
      String outputDirectory,
      int independentRuns,
      int numberOfCores,
      boolean buildReferenceFronts)
      throws IOException {
    Experiment<S, List<S>> experiment =
        new ExperimentBuilder<S, List<S>>(name)
            .setAlgorithmList(algorithms)
            .setProblemList(problems)
            .setReferenceFrontDirectory(referenceFrontDirectory)
            .setExperimentBaseDirectory(outputDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(indicators)
            .setIndependentRuns(independentRuns)
            .setNumberOfCores(numberOfCores)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    if (buildReferenceFronts) {
      // The non-dominated points of all the runs of all the algorithms, one front per problem
      new GenerateReferenceParetoFront(experiment).run();
    }
    new ComputeQualityIndicators<>(experiment).run();
  }

  private static <S extends Solution<?>> ExperimentAlgorithm<S, List<S>> algorithm(
      BaseLevelAlgorithm<S> baseLevelAlgorithm,
      String configuration,
      String tag,
      ExperimentProblem<S> problem,
      int run) {
    return new ExperimentAlgorithm<>(
        baseLevelAlgorithm.parse(configuration.split("\\s+")).build(), tag, problem, run);
  }

  private static YAMLParameterSpace binarySpace() {
    return new YAMLParameterSpace("NSGAIIBinary.yaml", new BinaryParameterFactory());
  }

  private static YAMLParameterSpace permutationSpace() {
    return new YAMLParameterSpace("NSGAIIPermutation.yaml", new PermutationParameterFactory());
  }

  private static String defaultConfiguration(String fileName) {
    try {
      return new ConfigurationFileReader("defaultConfigurations/" + fileName).getConfiguration(1);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
