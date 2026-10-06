package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;
import org.uma.evolver.meta.encoding.operator.TreeMutation;
import org.uma.evolver.meta.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.meta.encoding.util.GrammarConverter;
import org.uma.evolver.meta.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Code of tutorial E14, "Tree versus flat encoding" (see {@code
 * docs/tutorials/tree_versus_flat_encoding.rst}).
 *
 * <p>It looks at the two encodings of the meta-optimizer on the parameter space of NSGA-II ({@code
 * NSGAIIDouble.yaml}): the grammar that the tree encoding follows, how many of the variables of the
 * flat encoding are active in a configuration, and how many mutations of each encoding leave the
 * configuration unchanged. With the argument {@code train} it also runs the training of tutorial E3
 * with both encodings, stopped by time, several times, and summarizes them; with {@code summary} it
 * only summarizes trainings already run.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class TreeEncodingTutorial {

  private static final int SAMPLES = 10000;
  private static final int REPLICATIONS = 5;

  private TreeEncodingTutorial() {}

  public static void main(String[] args) throws IOException {
    JMetalRandom.getInstance().setSeed(1);
    ParameterSpace space = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());

    grammar(space);
    activeVariables(space);
    neutralMutations(space);

    if (args.length > 0 && args[0].equals("train")) {
      train("results/tutorial/tree-vs-flat");
    }
    if (args.length > 0 && (args[0].equals("train") || args[0].equals("summary"))) {
      summarize("results/tutorial/tree-vs-flat");
    }
  }

  /** Prints the grammar of the parameter space in BNF. */
  static void grammar(ParameterSpace space) {
    // [step-1-start]
    System.out.println(GrammarConverter.toBnf(space));
    // [step-1-end]
  }

  /** How many variables of the flat encoding are active in random configurations. */
  static void activeVariables(ParameterSpace space) {
    // [step-2-start]
    List<Parameter<?>> flat = ParameterManagement.parameterFlattening(space.topLevelParameters());
    TreeSolutionGenerator generator = new TreeSolutionGenerator(space);

    int[] activeInFlat = new int[SAMPLES];
    int[] nodesInTree = new int[SAMPLES];
    for (int i = 0; i < SAMPLES; i++) {
      activeInFlat[i] = activeIndices(space, flat, randomVector(flat.size()).variables()).size();
      nodesInTree[i] = generator.generate(2).toParameterArray().length / 2;
    }
    System.out.printf(
        Locale.ROOT,
        "Flat encoding: %d variables; active in a random configuration: %s%n",
        flat.size(),
        summary(activeInFlat));
    System.out.printf(
        Locale.ROOT, "Tree encoding: parameters in a random tree: %s%n", summary(nodesInTree));
    // [step-2-end]
  }

  /** How many mutations leave the decoded configuration unchanged, in each encoding. */
  static void neutralMutations(ParameterSpace space) {
    // [step-3-start]
    List<Parameter<?>> flat = ParameterManagement.parameterFlattening(space.topLevelParameters());
    // The mutation of the flat meta-optimizers of the tutorials: polynomial, with probability
    // 1/n per variable (mutationProbabilityFactor 1.0) and distribution index 20
    var flatMutation = new PolynomialMutation(1.0 / flat.size(), 20.0);
    int noVariableChanged = 0;
    int unchangedConfiguration = 0;
    for (int i = 0; i < SAMPLES; i++) {
      DoubleSolution solution = randomVector(flat.size());
      List<Double> before = new ArrayList<>(solution.variables());
      flatMutation.execute(solution);
      if (before.equals(solution.variables())) {
        noVariableChanged++;
      } else if (activeConfiguration(space, flat, before)
          .equals(activeConfiguration(space, flat, solution.variables()))) {
        unchangedConfiguration++;
      }
    }
    System.out.printf(
        Locale.ROOT,
        "Flat mutations: %.1f %% changed no variable, %.1f %% changed only variables that are"
            + " inactive or do not change the decoded value, %.1f %% changed the configuration%n",
        100.0 * noVariableChanged / SAMPLES,
        100.0 * unchangedConfiguration / SAMPLES,
        100.0 * (SAMPLES - noVariableChanged - unchangedConfiguration) / SAMPLES);

    // The mutation of the tree meta-optimizers of the tutorials: one node, distribution index 5
    TreeSolutionGenerator generator = new TreeSolutionGenerator(space);
    var treeMutation = new TreeMutation(1.0, 5.0, generator);
    int unchangedTree = 0;
    for (int i = 0; i < SAMPLES; i++) {
      DerivationTreeSolution tree = generator.generate(2);
      String before = String.join(" ", tree.toParameterArray());
      treeMutation.execute(tree);
      if (before.equals(String.join(" ", tree.toParameterArray()))) {
        unchangedTree++;
      }
    }
    System.out.printf(
        Locale.ROOT,
        "Tree mutations: %.1f %% left the configuration unchanged%n",
        100.0 * unchangedTree / SAMPLES);
    // [step-3-end]
  }

  /**
   * Runs the training of tutorial E3 (NSGA-II tuned for ZDT4), stopped after 2 minutes, {@value
   * #REPLICATIONS} times with each encoding, alternating them.
   */
  static void train(String outputDirectory) throws IOException {
    // [step-4-start]
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.load("TutorialZdt4BaseLevel.yaml");
    MetaSearchConfig flat = MetaOptimizerConfigurationReader.load("TutorialTimeNSGAIIMetaSearch.yaml");
    MetaSearchConfig tree =
        MetaOptimizerConfigurationReader.load("TutorialTimeTreeNSGAIIMetaSearch.yaml");

    for (int replication = 1; replication <= REPLICATIONS; replication++) {
      for (var encoding : List.of("flat", "tree")) {
        String directory = outputDirectory + "/" + encoding + "/run-" + replication;
        var request =
            new TrainingRequest(
                baseLevel, encoding.equals("flat") ? flat : tree, directory, 50, 50, null);
        Files.createDirectories(Path.of(directory));
        new TrainingRunner().run(request, Path.of(directory, "status.yaml"));
      }
    }
    // [step-4-end]
  }

  /**
   * Prints, for each training, the meta-evaluations it performed and the best NHV of its final
   * front, and the median of each encoding.
   */
  static void summarize(String outputDirectory) throws IOException {
    // [step-5-start]
    for (var encoding : List.of("flat", "tree")) {
      double[] bestNhv = new double[REPLICATIONS];
      int[] evaluations = new int[REPLICATIONS];
      for (int replication = 1; replication <= REPLICATIONS; replication++) {
        Path run = Path.of(outputDirectory, encoding, "run-" + replication);
        evaluations[replication - 1] = metaEvaluations(run.resolve("METADATA.txt"));
        bestNhv[replication - 1] = bestNhvOfFinalFront(run.resolve("INDICATORS.csv"));
        System.out.printf(
            Locale.ROOT,
            "%s run %d: %d meta-evaluations, best NHV %.4f%n",
            encoding,
            replication,
            evaluations[replication - 1],
            bestNhv[replication - 1]);
      }
      System.out.printf(
          Locale.ROOT,
          "%s: median of %d runs: %d meta-evaluations, best NHV %.4f%n",
          encoding,
          REPLICATIONS,
          (int) median(Arrays.stream(evaluations).asDoubleStream().toArray()),
          median(bestNhv));
    }
    // [step-5-end]
  }

  static int metaEvaluations(Path metadata) throws IOException {
    for (String line : Files.readAllLines(metadata)) {
      if (line.startsWith("Meta-evaluations performed: ")) {
        return Integer.parseInt(line.substring("Meta-evaluations performed: ".length()).trim());
      }
    }
    throw new IllegalStateException("No meta-evaluations in " + metadata);
  }

  /** The smallest NHV among the configurations of the last checkpoint of INDICATORS.csv. */
  static double bestNhvOfFinalFront(Path indicators) throws IOException {
    List<String> lines = Files.readAllLines(indicators);
    List<String> header = List.of(lines.get(0).split(","));
    int evaluationColumn = header.indexOf("Evaluation");
    int nhvColumn = header.indexOf("NHV");
    String lastEvaluation = lines.get(lines.size() - 1).split(",")[evaluationColumn];
    return lines.stream()
        .skip(1)
        .map(line -> line.split(","))
        .filter(fields -> fields[evaluationColumn].equals(lastEvaluation))
        .mapToDouble(fields -> Double.parseDouble(fields[nhvColumn]))
        .min()
        .orElseThrow();
  }

  static double median(double[] values) {
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    int middle = sorted.length / 2;
    return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
  }

  private static DoubleSolution randomVector(int size) {
    var solution = new DefaultDoubleSolution(Collections.nCopies(size, Bounds.create(0.0, 1.0)), 2, 0);
    for (int i = 0; i < size; i++) {
      solution.variables().set(i, JMetalRandom.getInstance().nextDouble());
    }
    return solution;
  }

  private static Set<Integer> activeIndices(
      ParameterSpace space, List<Parameter<?>> flat, List<Double> values) {
    return ParameterManagement.getActiveParameterIndices(space.topLevelParameters(), flat, values);
  }

  private static String activeConfiguration(
      ParameterSpace space, List<Parameter<?>> flat, List<Double> values) {
    return ParameterManagement.decodeActiveParametersToString(
            flat, values, activeIndices(space, flat, values))
        .toString();
  }

  private static String summary(int[] values) {
    int[] sorted = values.clone();
    Arrays.sort(sorted);
    double mean = Arrays.stream(values).average().orElse(0);
    return String.format(
        Locale.ROOT,
        "mean %.1f, median %d, min %d, max %d",
        mean,
        sorted[sorted.length / 2],
        sorted[0],
        sorted[sorted.length - 1]);
  }
}
