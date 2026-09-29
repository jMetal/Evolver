package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.example.tutorial.MetaOptimizationWorkflowTutorial.TunedConfiguration;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Code of tutorial E8, "Analyzing training results" (see {@code
 * docs/tutorials/analyzing_training_results.rst}).
 *
 * <p>It reads the configurations on the final front of a training run that tuned NSGA-II for the
 * ZDT problems ({@code tutorial-e8-request.yaml}), orders them by their NHV in the training (the
 * first one is the chosen configuration), and validates all of them, together with the standard
 * NSGA-II, on the same problems with a larger budget ({@value #MAX_EVALUATIONS} evaluations,
 * {@value #INDEPENDENT_RUNS} independent runs). The candidates are named {@code NSGAIIZDT1}, {@code
 * NSGAIIZDT2}, ... in that order.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class TrainingAnalysisTutorial {

  static final String TRAINING_DIRECTORY = "results/tutorial-e8/training";
  static final String OUTPUT_DIRECTORY = "results/tutorial-e8";
  static final int INDEPENDENT_RUNS = 15;
  static final int MAX_EVALUATIONS = 15000;
  static final int NUMBER_OF_CORES = 16;

  private static final int POPULATION_SIZE = 100;

  private TrainingAnalysisTutorial() {}

  public static void main(String[] args) throws IOException {
    // [step-1-start]
    List<TunedConfiguration> candidates =
        candidates(Path.of(TRAINING_DIRECTORY).resolve("VAR_CONF.txt"));
    Files.writeString(
        Path.of(OUTPUT_DIRECTORY, "best-configuration.txt"),
        candidates.get(0).configuration() + System.lineSeparator());
    // [step-1-end]

    System.out.println("Configurations on the final front, ordered by NHV:");
    for (int i = 0; i < candidates.size(); i++) {
      System.out.println(
          String.format(
              Locale.ROOT,
              "  NSGAIIZDT%d: NHV = %.4f, EP = %.4f",
              i + 1,
              candidates.get(i).normalizedHypervolume(),
              candidates.get(i).epsilon()));
    }

    validate(candidates, OUTPUT_DIRECTORY, INDEPENDENT_RUNS, MAX_EVALUATIONS, NUMBER_OF_CORES);
  }

  /** The configurations on the final front of a training run, ordered by NHV and then by EP. */
  static List<TunedConfiguration> candidates(Path varConfFile) throws IOException {
    List<TunedConfiguration> candidates =
        new ArrayList<>(MetaOptimizationWorkflowTutorial.finalConfigurations(varConfFile));
    candidates.sort(
        Comparator.comparingDouble(TunedConfiguration::normalizedHypervolume)
            .thenComparingDouble(TunedConfiguration::epsilon));
    return candidates;
  }

  static void validate(
      List<TunedConfiguration> candidates,
      String outputDirectory,
      int independentRuns,
      int maxEvaluations,
      int numberOfCores)
      throws IOException {
    // [step-2-start]
    List<ExperimentProblem<DoubleSolution>> problems =
        List.of(
            new ExperimentProblem<>(new ZDT1()).setReferenceFront("ZDT1.csv"),
            new ExperimentProblem<>(new ZDT2()).setReferenceFront("ZDT2.csv"),
            new ExperimentProblem<>(new ZDT3()).setReferenceFront("ZDT3.csv"),
            new ExperimentProblem<>(new ZDT4()).setReferenceFront("ZDT4.csv"),
            new ExperimentProblem<>(new ZDT6()).setReferenceFront("ZDT6.csv"));

    String defaultConfiguration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> problem : problems) {
        algorithms.add(nsgaII(defaultConfiguration, "NSGAII", problem, maxEvaluations, run));
        for (int i = 0; i < candidates.size(); i++) {
          algorithms.add(
              nsgaII(
                  candidates.get(i).configuration(),
                  "NSGAIIZDT" + (i + 1),
                  problem,
                  maxEvaluations,
                  run));
        }
      }
    }

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("validation")
            .setAlgorithmList(algorithms)
            .setProblemList(problems)
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(outputDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(
                List.of(
                    new Epsilon(), new PISAHypervolume(), new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(independentRuns)
            .setNumberOfCores(numberOfCores)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    // [step-2-end]
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> nsgaII(
      String configuration,
      String tag,
      ExperimentProblem<DoubleSolution> problem,
      int maxEvaluations,
      int run) {
    var nsgaII =
        new DoubleNSGAII(
            problem.getProblem(),
            POPULATION_SIZE,
            maxEvaluations,
            new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    return new ExperimentAlgorithm<>(
        nsgaII.parse(configuration.split("\\s+")).build(), tag, problem, run);
  }
}
