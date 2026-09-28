package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.agemoea.DoubleAGEMOEA;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.nsgaiii.DoubleNSGAIII;
import org.uma.evolver.algorithm.smsemoa.DoubleSMSEMOA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.problem.multiobjective.wfg.DefaultWFGSettings;
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
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Validation study of tutorial E7 (training sets, indicators and budgets): compares the NSGA-II
 * configuration found by {@code AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ}, trained on DTLZ1-7
 * with 10000 evaluations per problem, with the standard NSGA-II, NSGA-III, MOEA/D, SMS-EMOA and
 * AGE-MOEA (their default configurations), with a budget usual for these problems: 50000 evaluations.
 *
 * <p>The problems are DTLZ1-7, seen during training, and WFG1-9, not seen, all with three
 * objectives. The study runs {@value #INDEPENDENT_RUNS} independent runs per algorithm and problem,
 * enough for a tutorial; a real study should run 30 or more.
 *
 * <p>The standard NSGA-II comes first and the tuned configuration ({@code NSGAIIDTLZ}) last, the
 * pivot of the Wilcoxon tables generated from {@code QualityIndicatorSummary.csv} with
 * {@code scripts/wilcoxon_pivot_tables.py} (SAES).
 *
 * <p>Run it from the root of the repository once the training has finished and the chosen
 * configuration is saved in {@value #TUNED_CONFIGURATION_FILE}.
 */
public class TrainingSetsValidationTutorial {

  static final String TUNED_CONFIGURATION_FILE = "results/tutorial-e7/best-configuration.txt";
  static final String OUTPUT_DIRECTORY = "results/tutorial-e7";
  static final int INDEPENDENT_RUNS = 15;
  static final int MAX_EVALUATIONS = 50000;
  static final int NUMBER_OF_CORES = 16;

  private static final int POPULATION_SIZE = 100;
  // MOEA/D reads its weight vectors from W3D_100.dat, for three objectives and 100 subproblems
  private static final String WEIGHT_VECTOR_FILES_DIRECTORY = "resources/weightVectors";

  public static void main(String[] args) throws IOException {
    String tunedConfiguration =
        new ConfigurationFileReader(TUNED_CONFIGURATION_FILE).getConfiguration(1);
    run(tunedConfiguration, OUTPUT_DIRECTORY, INDEPENDENT_RUNS, MAX_EVALUATIONS, NUMBER_OF_CORES);
  }

  static void run(
      String tunedConfiguration,
      String outputDirectory,
      int independentRuns,
      int maxEvaluations,
      int numberOfCores)
      throws IOException {
    // [step-3-start]
    List<ExperimentProblem<DoubleSolution>> problems = new ArrayList<>();
    // Seen during training
    problems.add(new ExperimentProblem<>(new DTLZ1()).setReferenceFront("DTLZ1.3D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ2()).setReferenceFront("DTLZ2.3D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ3()).setReferenceFront("DTLZ3.3D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ4()).setReferenceFront("DTLZ4.3D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ5()).setReferenceFront("DTLZ5.3D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ6()).setReferenceFront("DTLZ6.3D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ7()).setReferenceFront("DTLZ7.3D.csv"));
    // Not seen during training
    DefaultWFGSettings.numberOfObjectives = 3;
    problems.add(new ExperimentProblem<>(new WFG1()).setReferenceFront("WFG1.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG2()).setReferenceFront("WFG2.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG3()).setReferenceFront("WFG3.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG4()).setReferenceFront("WFG4.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG5()).setReferenceFront("WFG5.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG6()).setReferenceFront("WFG6.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG7()).setReferenceFront("WFG7.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG8()).setReferenceFront("WFG8.3D.csv"));
    problems.add(new ExperimentProblem<>(new WFG9()).setReferenceFront("WFG9.3D.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> problem : problems) {
        Problem<DoubleSolution> p = problem.getProblem();
        algorithms.add(
            algorithm(
                new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, space("NSGAIIDouble.yaml")),
                defaultConfiguration("NSGAIIDoubleDefault.txt"),
                "NSGAII",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleNSGAIII(p, POPULATION_SIZE, maxEvaluations, space("NSGAIIIDouble.yaml")),
                defaultConfiguration("NSGAIIIDoubleDefault.txt"),
                "NSGAIII",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleMOEAD(
                    p,
                    POPULATION_SIZE,
                    maxEvaluations,
                    WEIGHT_VECTOR_FILES_DIRECTORY,
                    space("MOEADDouble.yaml")),
                defaultConfiguration("MOEADDoubleDefault.txt"),
                "MOEAD",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleSMSEMOA(p, POPULATION_SIZE, maxEvaluations, space("SMSEMOADouble.yaml")),
                defaultConfiguration("SMSEMOADoubleDefault.txt"),
                "SMSEMOA",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleAGEMOEA(p, POPULATION_SIZE, maxEvaluations, space("AGEMOEADouble.yaml")),
                defaultConfiguration("AGEMOEADoubleDefault.txt"),
                "AGEMOEA",
                problem,
                run));
        algorithms.add(
            algorithm(
                new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, space("NSGAIIDouble.yaml")),
                tunedConfiguration,
                "NSGAIIDTLZ",
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
                List.of(new Epsilon(), new PISAHypervolume(), new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(independentRuns)
            .setNumberOfCores(numberOfCores)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    // [step-3-end]
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

  private static YAMLParameterSpace space(String yamlParameterSpaceFile) {
    return new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
  }

  private static String defaultConfiguration(String fileName) {
    try {
      return new ConfigurationFileReader("defaultConfigurations/" + fileName).getConfiguration(1);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
