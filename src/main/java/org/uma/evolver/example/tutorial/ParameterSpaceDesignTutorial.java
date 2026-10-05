package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
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
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7_2D;
import org.uma.jmetal.problem.multiobjective.wfg.WFG1;
import org.uma.jmetal.problem.multiobjective.wfg.WFG2;
import org.uma.jmetal.problem.multiobjective.wfg.WFG3;
import org.uma.jmetal.problem.multiobjective.wfg.WFG4;
import org.uma.jmetal.problem.multiobjective.wfg.WFG5;
import org.uma.jmetal.problem.multiobjective.wfg.WFG6;
import org.uma.jmetal.problem.multiobjective.wfg.WFG7;
import org.uma.jmetal.problem.multiobjective.wfg.WFG8;
import org.uma.jmetal.problem.multiobjective.wfg.WFG9;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Validation study of tutorial E6 (designing your own parameter space): compares two configurations
 * of NSGA-II tuned for the bi-objective WFG problems with the same training, one found in the full
 * parameter space ({@code NSGAIIDouble.yaml}, the configuration of tutorial E9, {@code NSGAIIWFG})
 * and one in the much smaller space of Nebro et al., "Automatic Configuration of NSGA-II with jMetal
 * and irace" (GECCO 2019 Companion), {@code NSGAIIDoubleGECCO2019.yaml} ({@code NSGAIIWFGSmall}),
 * together with the standard NSGA-II.
 *
 * <p>The problems, budget, runs and indicators are those of tutorial E9: WFG1-9 (seen during
 * training) and DTLZ1-7 (not seen), with two objectives, {@value #MAX_EVALUATIONS} evaluations and
 * {@value #INDEPENDENT_RUNS} independent runs. Both tuned configurations are bundled in {@code
 * tunedConfigurations/}; pass two files as arguments (full space, small space) to validate your own.
 * Run it from the root of the repository.
 */
public class ParameterSpaceDesignTutorial {

  static final String FULL_SPACE_CONFIGURATION_FILE = "tunedConfigurations/NSGAIIWFG2D.txt";
  static final String SMALL_SPACE_CONFIGURATION_FILE =
      "tunedConfigurations/NSGAIIWFG2DGECCO2019.txt";
  static final String OUTPUT_DIRECTORY = "results/tutorial-e6";
  static final int INDEPENDENT_RUNS = 25;
  static final int MAX_EVALUATIONS = 25000;
  static final int NUMBER_OF_CORES = 16;

  private static final int POPULATION_SIZE = 100;

  public static void main(String[] args) throws IOException {
    String fullSpaceFile = args.length > 1 ? args[0] : FULL_SPACE_CONFIGURATION_FILE;
    String smallSpaceFile = args.length > 1 ? args[1] : SMALL_SPACE_CONFIGURATION_FILE;
    run(
        new ConfigurationFileReader(fullSpaceFile).getConfiguration(1),
        new ConfigurationFileReader(smallSpaceFile).getConfiguration(1),
        OUTPUT_DIRECTORY,
        INDEPENDENT_RUNS,
        MAX_EVALUATIONS,
        NUMBER_OF_CORES);
  }

  static void run(
      String fullSpaceConfiguration,
      String smallSpaceConfiguration,
      String outputDirectory,
      int independentRuns,
      int maxEvaluations,
      int numberOfCores)
      throws IOException {
    // [step-1-start]
    List<ExperimentProblem<DoubleSolution>> problems = new ArrayList<>();
    // Seen during training
    problems.add(new ExperimentProblem<>(new WFG1(2, 4, 2)).setReferenceFront("WFG1.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG2(2, 4, 2)).setReferenceFront("WFG2.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG3(2, 4, 2)).setReferenceFront("WFG3.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG4(2, 4, 2)).setReferenceFront("WFG4.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG5(2, 4, 2)).setReferenceFront("WFG5.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG6(2, 4, 2)).setReferenceFront("WFG6.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG7(2, 4, 2)).setReferenceFront("WFG7.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG8(2, 4, 2)).setReferenceFront("WFG8.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG9(2, 4, 2)).setReferenceFront("WFG9.2D.csv"));
    // Not seen during training
    problems.add(new ExperimentProblem<>(new DTLZ1_2D()).setReferenceFront("DTLZ1.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ2_2D()).setReferenceFront("DTLZ2.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ3_2D()).setReferenceFront("DTLZ3.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ4_2D()).setReferenceFront("DTLZ4.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ5_2D()).setReferenceFront("DTLZ5.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ6_2D()).setReferenceFront("DTLZ6.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ7_2D()).setReferenceFront("DTLZ7.2D.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> problem : problems) {
        var p = (DoubleProblem) problem.getProblem();
        algorithms.add(
            algorithm(
                new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, nsgaiiSpace()),
                defaultConfiguration("NSGAIIDoubleDefault.txt"),
                "NSGAII",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, nsgaiiSpace()),
                fullSpaceConfiguration,
                "NSGAIIWFG",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, smallSpace()),
                smallSpaceConfiguration,
                "NSGAIIWFGSmall",
                problem,
                run));
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
                    new Epsilon(),
                    new Spread(),
                    new PISAHypervolume(),
                    new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(independentRuns)
            .setNumberOfCores(numberOfCores)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    // [step-1-end]
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> algorithm(
      BaseLevelAlgorithm<DoubleSolution> baseLevelAlgorithm,
      String configuration,
      String tag,
      ExperimentProblem<DoubleSolution> problem,
      int run) {
    return new ExperimentAlgorithm<>(
        baseLevelAlgorithm.parse(configuration.split("\\s+")).build(), tag, problem, run);
  }

  private static YAMLParameterSpace nsgaiiSpace() {
    return new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
  }

  private static YAMLParameterSpace smallSpace() {
    return new YAMLParameterSpace("NSGAIIDoubleGECCO2019.yaml", new DoubleParameterFactory());
  }

  private static String defaultConfiguration(String fileName) {
    try {
      return new ConfigurationFileReader("defaultConfigurations/" + fileName).getConfiguration(1);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
