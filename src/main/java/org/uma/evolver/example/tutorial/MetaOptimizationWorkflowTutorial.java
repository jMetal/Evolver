package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.cli.training.BaseLevelConfig;
import org.uma.evolver.cli.training.BaseLevelConfigurationReader;
import org.uma.evolver.cli.training.MetaOptimizerConfigurationReader;
import org.uma.evolver.cli.training.MetaSearchConfig;
import org.uma.evolver.cli.training.TrainingRequest;
import org.uma.evolver.cli.training.TrainingRunner;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Code of tutorial E3, "Meta-optimization workflow" (see {@code
 * docs/tutorials/meta_optimization_workflow.rst}).
 *
 * <p>It tunes NSGA-II for the ZDT4 problem with NSGA-II as meta-optimizer: it loads the base-level
 * and meta-search configurations, runs the training with {@link TrainingRunner}, reads the best
 * meta-optimizer's final front, chooses a configuration, and compares it with the default NSGA-II
 * configuration on ZDT4, writing both fronts.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class MetaOptimizationWorkflowTutorial {

  /** A configuration found by the training, with its indicator values. */
  public record TunedConfiguration(double epsilon, double normalizedHypervolume, String configuration) {}



  /** Evaluations of each run when comparing the chosen and the default configurations (step 5). */
  public static final int VALIDATION_EVALUATIONS = 20000;

  private MetaOptimizationWorkflowTutorial() {}

  public static void main(String[] args) throws IOException {
    // [step-1-start]
    BaseLevelConfig baseLevel = BaseLevelConfigurationReader.load("TutorialZdt4BaseLevel.yaml");

    System.out.println(
        "Base level: "
            + baseLevel.algorithmName()
            + " on "
            + baseLevel.trainingProblemNames()
            + " with "
            + baseLevel.trainingEvaluations()
            + " evaluations, indicators "
            + baseLevel.indicatorNames());
    // [step-1-end]

    // [step-2-start]
    MetaSearchConfig metaSearch =
        MetaOptimizerConfigurationReader.load("TutorialNSGAIIMetaSearch.yaml");

    System.out.println(
        "Meta-optimizer: "
            + metaSearch.algorithm()
            + ", "
            + metaSearch.metaMaxEvaluations()
            + " configurations");
    // [step-2-end]

    run(baseLevel, metaSearch, "results/tutorial/E3");
  }

  /**
   * Runs the training and the comparison steps of the tutorial.
   *
   * @param baseLevel the base level: the algorithm to tune, its training problem and indicators
   * @param metaSearch the meta-optimizer and its budget
   * @param outputDirectory where the training writes its results
   */
  public static void run(BaseLevelConfig baseLevel, MetaSearchConfig metaSearch, String outputDirectory)
      throws IOException {
    // [step-3-start]
    var request = new TrainingRequest(baseLevel, metaSearch, outputDirectory, 50, 50, null);

    long start = System.currentTimeMillis();
    Path results = new TrainingRunner().run(request, Path.of(outputDirectory, "status.yaml"));
    long seconds = (System.currentTimeMillis() - start) / 1000;

    System.out.println("Training finished in " + seconds + " s; results in " + results);
    // [step-3-end]

    // [step-4-start]
    List<TunedConfiguration> metaFront = finalConfigurations(results.resolve("VAR_CONF.txt"));
    metaFront.sort(
        Comparator.comparingDouble(TunedConfiguration::normalizedHypervolume)
            .thenComparingDouble(TunedConfiguration::epsilon));

    System.out.println("Configurations on the meta-optimizer's final front:");
    for (TunedConfiguration configuration : metaFront) {
      System.out.printf(
          Locale.ROOT,
          "  NHV = %.4f, EP = %.4f%n",
          configuration.normalizedHypervolume(),
          configuration.epsilon());
    }

    TunedConfiguration chosen = metaFront.get(0);
    System.out.println("Chosen (lowest NHV): " + chosen.configuration());
    // [step-4-end]

    // [step-5-start]
    String defaultConfiguration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    Path validation = Path.of(outputDirectory, "validation");
    System.out.println(
        "Default configuration on ZDT4: "
            + runAndWriteFront(
                defaultConfiguration, VALIDATION_EVALUATIONS, validation.resolve("default")));
    System.out.println(
        "Chosen configuration on ZDT4:  "
            + runAndWriteFront(
                chosen.configuration(), VALIDATION_EVALUATIONS, validation.resolve("tuned")));
    // [step-5-end]
  }

  /**
   * Reads the configurations of the last checkpoint of a training's {@code VAR_CONF.txt} file: the
   * meta-optimizer's final front, that is, the configurations of its final population that are not
   * dominated in the indicators used as objectives.
   *
   * @param varConfFile the {@code VAR_CONF.txt} file written by the training
   * @return the configurations, with the indicator values they obtained during the training
   */
  public static List<TunedConfiguration> finalConfigurations(Path varConfFile)
      throws IOException {
    List<TunedConfiguration> configurations = new ArrayList<>();
    for (String line : Files.readAllLines(varConfFile)) {
      if (line.startsWith("# Evaluation")) {
        configurations.clear();
      } else if (line.contains(" | ")) {
        configurations.add(parse(line));
      }
    }
    return configurations;
  }

  private static TunedConfiguration parse(String line) {
    String[] indicatorsAndConfiguration = line.split(" \\| ", 2);
    String[] indicators = indicatorsAndConfiguration[0].trim().split(" ");
    return new TunedConfiguration(
        Double.parseDouble(indicators[0].substring(indicators[0].indexOf('=') + 1)),
        Double.parseDouble(indicators[1].substring(indicators[1].indexOf('=') + 1)),
        indicatorsAndConfiguration[1].trim());
  }

  private static List<DoubleSolution> runOnZDT4(String configuration, int evaluations) {
    var nsgaii =
        new DoubleNSGAII(
            new ZDT4(),
            100,
            evaluations,
            new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    nsgaii.parse(configuration.split(" "));

    EvolutionaryAlgorithm<DoubleSolution> algorithm = nsgaii.build();
    algorithm.run();
    return algorithm.result();
  }

  private static String runAndWriteFront(String configuration, int evaluations, Path directory)
      throws IOException {
    JMetalRandom.getInstance().setSeed(1);
    List<DoubleSolution> front = runOnZDT4(configuration, evaluations);

    Files.createDirectories(directory);
    new SolutionListOutput(front)
        .setVarFileOutputContext(new DefaultFileOutputContext(directory + "/VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext(directory + "/FUN.csv", ","))
        .print();
    return BaseLevelAlgorithmsTutorial.indicators(front, "ZDT4.csv");
  }
}
