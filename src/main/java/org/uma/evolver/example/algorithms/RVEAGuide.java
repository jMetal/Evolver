package org.uma.evolver.example.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.rvea.DoubleRVEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.problem.multiobjective.maf.MaF08;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Code of the algorithm guide of RVEA (see {@code docs/algorithms/rvea.rst}).
 *
 * <p>Step 1 runs RVEA with its default configuration on DTLZ2. Step 2 compares RVEA, RVEA* and
 * iRVEA, each with its default configuration, with NSGA-II on problems chosen to show where each
 * variant does well and where it does badly: a regular front (DTLZ2), a many-objective one (DTLZ2
 * with six objectives), a degenerate one (DTLZ5), a disconnected one (DTLZ7), an inverted one
 * (DTLZ2Minus), one whose Pareto set is a polygon in a two-dimensional decision space (MaF08 with
 * three objectives, with the {@value #MAX_EVALUATIONS_MAF08} evaluations of the iRVEA paper) and a
 * bi-objective convex one (ZDT1). The population size is 100 in every case,
 * with the reference vectors of {@code resources/weightVectors}.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * guide includes; keep them when editing this class.
 */
public class RVEAGuide {

  static final String OUTPUT_DIRECTORY = "results/algorithm-guides/rvea";
  static final int INDEPENDENT_RUNS = 15;
  static final int MAX_EVALUATIONS = 25000;
  static final int MAX_EVALUATIONS_MANY_OBJECTIVES = 50000;
  static final int MAX_EVALUATIONS_MAF08 = 60000;
  static final int NUMBER_OF_CORES = 16;
  static final int POPULATION_SIZE = 100;
  static final String WEIGHT_VECTORS = "resources/weightVectors";

  /** The variants of the RVEA family, as tag and default configuration file. */
  static final List<List<String>> RVEA_VARIANTS =
      List.of(
          List.of("RVEA", "defaultConfigurations/RVEADoubleDefault.txt"),
          List.of("RVEAStar", "defaultConfigurations/RVEAStarDoubleDefault.txt"),
          List.of("iRVEA", "defaultConfigurations/IRVEADoubleDefault.txt"));

  private RVEAGuide() {}

  public static void main(String[] args) throws IOException {
    runOnDTLZ2();
    compare(OUTPUT_DIRECTORY, INDEPENDENT_RUNS, 1.0, NUMBER_OF_CORES);
  }

  /** Step 1: RVEA with its default configuration on DTLZ2. */
  static List<DoubleSolution> runOnDTLZ2() throws IOException {
    // [step-1-start]
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/RVEADoubleDefault.txt")
            .getConfiguration(1);

    var rvea =
        new DoubleRVEA(
            new DTLZ2(),
            POPULATION_SIZE,
            MAX_EVALUATIONS,
            WEIGHT_VECTORS,
            new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory()));
    rvea.parse(configuration.split("\\s+"));

    Algorithm<List<DoubleSolution>> algorithm = rvea.build();
    algorithm.run();
    List<DoubleSolution> front = algorithm.result();
    // [step-1-end]
    System.out.println(algorithm.name() + " on DTLZ2: " + front.size() + " solutions");
    return front;
  }

  /**
   * Step 2: the three variants and NSGA-II on the seven problems. The evaluation budgets are those of
   * the constants, multiplied by {@code budgetFactor} (1.0 in the guide; smaller in the test).
   */
  static void compare(
      String outputDirectory, int independentRuns, double budgetFactor, int numberOfCores)
      throws IOException {
    // [step-2-start]
    List<ExperimentProblem<DoubleSolution>> problems =
        List.of(
            new ExperimentProblem<>(new DTLZ2(), "DTLZ2").setReferenceFront("DTLZ2.3D.csv"),
            new ExperimentProblem<>(new DTLZ2(15, 6), "DTLZ2.6D")
                .setReferenceFront("DTLZ2.6D.csv"),
            new ExperimentProblem<>(new DTLZ5(), "DTLZ5").setReferenceFront("DTLZ5.3D.csv"),
            new ExperimentProblem<>(new DTLZ7(), "DTLZ7").setReferenceFront("DTLZ7.3D.csv"),
            new ExperimentProblem<>(new DTLZ2Minus(), "DTLZ2Minus")
                .setReferenceFront("DTLZ2Minus.3D.csv"),
            new ExperimentProblem<>(maF08(), "MaF08").setReferenceFront("MaF08.3D.csv"),
            new ExperimentProblem<>(new ZDT1(), "ZDT1").setReferenceFront("ZDT1.csv"));

    String nsgaIIConfiguration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < independentRuns; run++) {
      for (ExperimentProblem<DoubleSolution> problem : problems) {
        int evaluations =
            (int)
                (budgetFactor
                    * switch (problem.getTag()) {
                      case "DTLZ2.6D" -> MAX_EVALUATIONS_MANY_OBJECTIVES;
                      case "MaF08" -> MAX_EVALUATIONS_MAF08;
                      default -> MAX_EVALUATIONS;
                    });
        for (List<String> variant : RVEA_VARIANTS) {
          String configuration =
              new ConfigurationFileReader(variant.get(1)).getConfiguration(1);
          algorithms.add(
              new ExperimentAlgorithm<>(
                  rvea(problem.getProblem(), evaluations, configuration),
                  variant.get(0),
                  problem,
                  run));
        }
        algorithms.add(
            new ExperimentAlgorithm<>(
                nsgaII(problem.getProblem(), evaluations, nsgaIIConfiguration),
                "NSGAII",
                problem,
                run));
      }
    }

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("comparison")
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

  /**
   * MaF08 with three objectives and the decision space of the MaF test suite, {@code [-10000,
   * 10000]}. jMetal's class bounds the variables to {@code [0, 1]}, which contains only a fifth of
   * the polygon that is its Pareto set (the vertices are at distance 1 from the origin), so most of
   * its Pareto front could not be reached. Fixed in jMetal's develop branch after 7.6 (db77a48ff):
   * remove this method when Evolver depends on the first release that includes the fix.
   */
  private static MaF08 maF08() {
    MaF08 problem = new MaF08(2, 3);
    problem.variableBounds(List.of(-10000.0, -10000.0), List.of(10000.0, 10000.0));
    return problem;
  }

  private static Algorithm<List<DoubleSolution>> rvea(
      Problem<DoubleSolution> problem, int maxEvaluations, String configuration) {
    var rvea =
        new DoubleRVEA(
            problem,
            POPULATION_SIZE,
            maxEvaluations,
            WEIGHT_VECTORS,
            new YAMLParameterSpace("RVEADouble.yaml", new DoubleParameterFactory()));
    return rvea.parse(configuration.split("\\s+")).build();
  }

  private static Algorithm<List<DoubleSolution>> nsgaII(
      Problem<DoubleSolution> problem, int maxEvaluations, String configuration) {
    var nsgaII =
        new DoubleNSGAII(
            problem,
            POPULATION_SIZE,
            maxEvaluations,
            new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    return nsgaII.parse(configuration.split("\\s+")).build();
  }
}
