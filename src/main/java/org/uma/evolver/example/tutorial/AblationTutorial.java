package org.uma.evolver.example.tutorial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.ConfigurationVariants;
import org.uma.evolver.parameter.ParameterSpace;
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
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Ablation study of tutorial E12, "Ablation: which components matter" (see {@code
 * docs/tutorials/ablation.rst}).
 *
 * <p>It takes the NSGA-II configuration tuned in tutorial E8 ({@value #TUNED_CONFIGURATION_FILE})
 * and derives, with {@link ConfigurationVariants}, one variant per component in which that
 * component is set back to the value of the default configuration. It then validates the default
 * configuration, the tuned one and the variants together, with the protocol of tutorial E8: WFG1-9
 * and DTLZ1-7 with two objectives, {@value #MAX_EVALUATIONS} evaluations and {@value
 * #INDEPENDENT_RUNS} independent runs, with the hypervolume and IGD+.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class. Run it from the root of the
 * repository.
 */
public class AblationTutorial {

  static final String TUNED_CONFIGURATION_FILE = "tunedConfigurations/NSGAIIWFG2D.txt";
  static final String DEFAULT_CONFIGURATION_FILE = "defaultConfigurations/NSGAIIDoubleDefault.txt";
  static final String OUTPUT_DIRECTORY = "results/tutorial-ablation";
  static final int INDEPENDENT_RUNS = 25;
  static final int MAX_EVALUATIONS = 25000;
  static final int NUMBER_OF_CORES = 16;

  private static final int POPULATION_SIZE = 100;

  // [step-1-start]
  /** The components of the study, each one with the parameters that are set back to the default. */
  static final Map<String, List<String>> COMPONENTS = new LinkedHashMap<>();

  static {
    COMPONENTS.put("NoArchive", List.of("algorithmResult"));
    COMPONENTS.put("DefaultOffspring", List.of("offspringPopulationSize"));
    COMPONENTS.put("DefaultInitialization", List.of("createInitialSolutions"));
    COMPONENTS.put(
        "DefaultCrossover", List.of("crossover", "crossoverProbability", "crossoverRepairStrategy"));
    COMPONENTS.put(
        "DefaultMutation", List.of("mutation", "mutationProbabilityFactor", "mutationRepairStrategy"));
    COMPONENTS.put("DefaultSelection", List.of("selection", "selectionTournamentSize"));
  }
  // [step-1-end]

  private AblationTutorial() {}

  public static void main(String[] args) throws IOException {
    String tuned = new ConfigurationFileReader(TUNED_CONFIGURATION_FILE).getConfiguration(1);
    String defaults = new ConfigurationFileReader(DEFAULT_CONFIGURATION_FILE).getConfiguration(1);

    Map<String, String> configurations = configurations(tuned, defaults);
    configurations.forEach((tag, configuration) -> System.out.println(tag + ": " + configuration));

    run(configurations, OUTPUT_DIRECTORY, INDEPENDENT_RUNS, MAX_EVALUATIONS, NUMBER_OF_CORES);
  }

  /**
   * The configurations of the study: the default one, the tuned one and a variant of the tuned one
   * per component of {@link #COMPONENTS}, with that component set back to its default value.
   *
   * @return the configurations by tag, in the order of the tables
   */
  static Map<String, String> configurations(String tuned, String defaults) {
    // [step-2-start]
    ParameterSpace space = nsgaiiSpace();
    Map<String, String> defaultValues = ConfigurationVariants.parameterValues(defaults);

    Map<String, String> configurations = new LinkedHashMap<>();
    configurations.put("Default", defaults);
    COMPONENTS.forEach(
        (tag, parameters) -> {
          Map<String, String> changes = new LinkedHashMap<>();
          for (String parameter : parameters) {
            changes.put(parameter, defaultValues.get(parameter));
          }
          configurations.put(tag, ConfigurationVariants.derive(space, tuned, changes, defaults));
        });
    configurations.put("Tuned", tuned);
    // [step-2-end]
    return configurations;
  }

  static void run(
      Map<String, String> configurations,
      String outputDirectory,
      int independentRuns,
      int maxEvaluations,
      int numberOfCores)
      throws IOException {
    // [step-3-start]
    List<ExperimentProblem<DoubleSolution>> problems = problems();

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> problem : problems) {
        var p = (DoubleProblem) problem.getProblem();
        for (var entry : configurations.entrySet()) {
          var nsgaii =
              new DoubleNSGAII(p, POPULATION_SIZE, maxEvaluations, nsgaiiSpace())
                  .parse(entry.getValue().split("\\s+"))
                  .build();
          algorithms.add(new ExperimentAlgorithm<>(nsgaii, entry.getKey(), problem, run));
        }
      }
    }

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ablation")
            .setAlgorithmList(algorithms)
            .setProblemList(problems)
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(outputDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(List.of(new PISAHypervolume(), new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(independentRuns)
            .setNumberOfCores(numberOfCores)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    // [step-3-end]
  }

  /** The problems of tutorial E8: WFG1-9 (seen during the training) and DTLZ1-7 (not seen). */
  static List<ExperimentProblem<DoubleSolution>> problems() {
    List<ExperimentProblem<DoubleSolution>> problems = new ArrayList<>();
    problems.add(new ExperimentProblem<>(new WFG1(2, 4, 2)).setReferenceFront("WFG1.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG2(2, 4, 2)).setReferenceFront("WFG2.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG3(2, 4, 2)).setReferenceFront("WFG3.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG4(2, 4, 2)).setReferenceFront("WFG4.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG5(2, 4, 2)).setReferenceFront("WFG5.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG6(2, 4, 2)).setReferenceFront("WFG6.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG7(2, 4, 2)).setReferenceFront("WFG7.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG8(2, 4, 2)).setReferenceFront("WFG8.2D.csv"));
    problems.add(new ExperimentProblem<>(new WFG9(2, 4, 2)).setReferenceFront("WFG9.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ1_2D()).setReferenceFront("DTLZ1.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ2_2D()).setReferenceFront("DTLZ2.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ3_2D()).setReferenceFront("DTLZ3.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ4_2D()).setReferenceFront("DTLZ4.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ5_2D()).setReferenceFront("DTLZ5.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ6_2D()).setReferenceFront("DTLZ6.2D.csv"));
    problems.add(new ExperimentProblem<>(new DTLZ7_2D()).setReferenceFront("DTLZ7.2D.csv"));
    return problems;
  }

  private static YAMLParameterSpace nsgaiiSpace() {
    return new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
  }
}
