package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
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
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Code of tutorial E14, "Tuning with irace" (see {@code docs/tutorials/tuning_with_irace.rst}).
 *
 * <p>It reads the best configuration found by irace from its standard output ({@code
 * irace.stdout.txt}), saves it, and applies it to the ZDT problems it was tuned for, together with
 * the standard NSGA-II as a reference: {@value #INDEPENDENT_RUNS} runs of {@value
 * #MAX_EVALUATIONS} evaluations on each problem, whose fronts are then plotted.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class IraceTutorial {

  static final String IRACE_DIRECTORY = "results/tutorial-e14";
  static final String IRACE_OUTPUT = IRACE_DIRECTORY + "/execdir-1/irace.stdout.txt";
  static final int INDEPENDENT_RUNS = 15;
  static final int MAX_EVALUATIONS = 15000;
  static final int NUMBER_OF_CORES = 16;

  /** The line of irace's output after which the best configurations are listed. */
  static final String BEST_CONFIGURATIONS_HEADER = "# Best configurations as commandlines";

  private static final int POPULATION_SIZE = 100;

  private IraceTutorial() {}

  public static void main(String[] args) throws IOException {
    // [step-1-start]
    String configuration = bestConfiguration(Path.of(IRACE_OUTPUT));
    Files.writeString(
        Path.of(IRACE_DIRECTORY, "best-configuration.txt"), configuration + System.lineSeparator());
    // [step-1-end]
    System.out.println("Best configuration found by irace:");
    System.out.println("  " + configuration);

    apply(configuration, IRACE_DIRECTORY, INDEPENDENT_RUNS, MAX_EVALUATIONS, NUMBER_OF_CORES);
  }

  /**
   * Reads the best configuration from irace's standard output: the first line after {@value
   * #BEST_CONFIGURATIONS_HEADER}, without the configuration identifier that begins it.
   */
  static String bestConfiguration(Path iraceOutput) throws IOException {
    List<String> lines = Files.readAllLines(iraceOutput);
    int header = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).startsWith(BEST_CONFIGURATIONS_HEADER)) {
        header = i;
      }
    }
    if (header == -1 || header + 1 >= lines.size()) {
      throw new JMetalException("No best configuration found in " + iraceOutput);
    }
    return lines.get(header + 1).trim().replaceFirst("^\\d+\\s+", "");
  }

  static void apply(
      String configuration,
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
        algorithms.add(nsgaII(configuration, "NSGAIIIrace", problem, maxEvaluations, run));
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
