package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.mopso.BaseMOPSO;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
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
 * Validation study of tutorial E9 (validating a configuration), which repeats the one of Nebro et
 * al., "Automatic Configuration of NSGA-II with jMetal and irace" (GECCO 2019 Companion) with a
 * configuration found by Evolver instead of irace.
 *
 * <p>It compares the standard NSGA-II, SMPSO (Evolver's MOPSO with its SMPSO configuration) and
 * NSGA-II with a configuration tuned for the bi-objective WFG problems ({@code NSGAIIWFG}), with
 * {@value #MAX_EVALUATIONS} evaluations and {@value #INDEPENDENT_RUNS} independent runs, on WFG1-9
 * (seen during training) and DTLZ1-7 (not seen), all with two objectives. The indicators are those
 * of the paper: EP, Spread, HV and IGD+.
 *
 * <p>By default the tuned configuration is the one bundled in {@value #TUNED_CONFIGURATION_FILE},
 * found with {@code tutorial-e9-request.yaml}; pass another file as the first argument to validate
 * your own (for instance, {@code results/tutorial-e9/best-configuration.txt}). Run it from the root
 * of the repository.
 */
public class ValidationTutorial {

  static final String TUNED_CONFIGURATION_FILE = "tunedConfigurations/NSGAIIWFG2D.txt";
  static final String OUTPUT_DIRECTORY = "results/tutorial-e9";
  static final int INDEPENDENT_RUNS = 25;
  static final int MAX_EVALUATIONS = 25000;
  static final int NUMBER_OF_CORES = 16;

  private static final int POPULATION_SIZE = 100;

  public static void main(String[] args) throws IOException {
    String configurationFile = args.length > 0 ? args[0] : TUNED_CONFIGURATION_FILE;
    String tunedConfiguration = new ConfigurationFileReader(configurationFile).getConfiguration(1);
    run(tunedConfiguration, OUTPUT_DIRECTORY, INDEPENDENT_RUNS, MAX_EVALUATIONS, NUMBER_OF_CORES);
  }

  static void run(
      String tunedConfiguration,
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
                new BaseMOPSO(p, POPULATION_SIZE, maxEvaluations, mopsoSpace()),
                defaultConfiguration("SMSPSODefault.txt"),
                "SMPSO",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, nsgaiiSpace()),
                tunedConfiguration,
                "NSGAIIWFG",
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

  private static YAMLParameterSpace mopsoSpace() {
    return new YAMLParameterSpace("MOPSO.yaml", new MOPSOParameterFactory());
  }

  private static String defaultConfiguration(String fileName) {
    try {
      return new ConfigurationFileReader("defaultConfigurations/" + fileName).getConfiguration(1);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
