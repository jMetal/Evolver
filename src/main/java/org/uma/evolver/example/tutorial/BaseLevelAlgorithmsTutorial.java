package org.uma.evolver.example.tutorial;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.nsgaii.BinaryNSGAII;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Code of tutorial E2, "Base-level algorithms" (see {@code docs/tutorials/base_level_algorithms.rst}).
 *
 * <p>It configures and runs Evolver's NSGA-II: from a configuration string, with a different
 * configuration, with a configuration read from a file, on another problem, and on a binary
 * problem. It uses Evolver the way it can be used on its own, without meta-optimization.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class BaseLevelAlgorithmsTutorial {

  /** Evaluation budget of every run in the tutorial. */
  public static final int MAXIMUM_NUMBER_OF_EVALUATIONS = 25000;

  private BaseLevelAlgorithmsTutorial() {}

  public static void main(String[] args) throws IOException {
    run(MAXIMUM_NUMBER_OF_EVALUATIONS, "results/tutorial/E2");
  }

  /**
   * Runs every step of the tutorial.
   *
   * @param maximumNumberOfEvaluations the evaluation budget of each run
   * @param outputDirectory where step 2 writes the {@code VAR.csv}/{@code FUN.csv} files
   */
  public static void run(int maximumNumberOfEvaluations, String outputDirectory)
      throws IOException {
    JMetalRandom.getInstance().setSeed(1);

    // [step-1-start]
    String[] configuration =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--offspringPopulationSize 100 "
                + "--variation crossoverAndMutationVariation "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split(" ");

    var parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
    var nsgaii = new DoubleNSGAII(new ZDT1(), 100, maximumNumberOfEvaluations, parameterSpace);
    nsgaii.parse(configuration);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = nsgaii.build();
    algorithm.run();
    // [step-1-end]

    // [step-2-start]
    List<DoubleSolution> front = algorithm.result();
    System.out.println(
        front.size() + " solutions after " + algorithm.numberOfEvaluations() + " evaluations");

    new File(outputDirectory).mkdirs();
    new SolutionListOutput(front)
        .setVarFileOutputContext(new DefaultFileOutputContext(outputDirectory + "/VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext(outputDirectory + "/FUN.csv", ","))
        .print();

    System.out.println("Standard configuration on ZDT1: " + indicators(front, "ZDT1.csv"));
    // [step-2-end]

    // [step-3-start]
    String[] otherConfiguration =
        ("--algorithmResult externalArchive "
                + "--populationSizeWithArchive 20 "
                + "--archiveType crowdingDistanceArchive "
                + "--createInitialSolutions latinHypercubeSampling "
                + "--offspringPopulationSize 10 "
                + "--variation crossoverAndMutationVariation "
                + "--crossover blxAlpha "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--blxAlphaCrossoverAlpha 0.5 "
                + "--mutation uniform "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--uniformMutationPerturbation 0.5 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split(" ");

    var otherNSGAII =
        new DoubleNSGAII(
            new ZDT1(),
            100,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory()));
    otherNSGAII.parse(otherConfiguration);

    Algorithm<List<DoubleSolution>> otherAlgorithm = otherNSGAII.build();
    otherAlgorithm.run();
    System.out.println(
        "Other configuration on ZDT1:    " + indicators(otherAlgorithm.result(), "ZDT1.csv"));
    // [step-3-end]

    // [step-4-start]
    var configurations = new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt");
    String[] defaultConfiguration = configurations.getConfiguration(1).split(" ");
    System.out.println(configurations.getNumberOfConfigurations() + " configuration(s) in the file");
    // [step-4-end]

    // [step-5-start]
    BaseLevelAlgorithm<DoubleSolution> nsgaiiForZDT2 =
        nsgaii.createInstance(new ZDT2(), maximumNumberOfEvaluations);
    nsgaiiForZDT2.parse(defaultConfiguration);

    Algorithm<List<DoubleSolution>> algorithmForZDT2 = nsgaiiForZDT2.build();
    algorithmForZDT2.run();
    System.out.println(
        "Default configuration on ZDT2:  " + indicators(algorithmForZDT2.result(), "ZDT2.csv"));
    // [step-5-end]

    // [step-6-start]
    String[] binaryConfiguration =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--offspringPopulationSize 100 "
                + "--variation crossoverAndMutationVariation "
                + "--crossover HUX "
                + "--crossoverProbability 0.9 "
                + "--mutation bitFlip "
                + "--mutationProbabilityFactor 1.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split(" ");

    var binaryNSGAII =
        new BinaryNSGAII(
            new OneZeroMax(512),
            100,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace("NSGAIIBinary.yaml", new BinaryParameterFactory()));
    binaryNSGAII.parse(binaryConfiguration);

    EvolutionaryAlgorithm<BinarySolution> binaryAlgorithm = binaryNSGAII.build();
    binaryAlgorithm.run();

    BinarySolution first = binaryAlgorithm.result().get(0);
    System.out.println(
        binaryAlgorithm.result().size()
            + " binary solutions; the first one has objectives "
            + first.objectives()[0]
            + " and "
            + first.objectives()[1]);
    // [step-6-end]
  }

  /**
   * Computes the Epsilon (EP) and normalized hypervolume (NHV) indicators of a front against the
   * reference front of its problem, both to be minimized.
   *
   * @param front the solutions found by an algorithm
   * @param referenceFrontFileName a file under {@code resources/referenceFronts/}
   * @return the two values, as {@code "EP = ..., NHV = ..."}
   */
  public static String indicators(List<DoubleSolution> front, String referenceFrontFileName)
      throws IOException {
    double[][] referenceFront =
        VectorUtils.readVectors("resources/referenceFronts/" + referenceFrontFileName, ",");
    double[][] objectives = SolutionListUtils.getMatrixWithObjectiveValues(front);

    double epsilon = new Epsilon(referenceFront).compute(objectives);
    double normalizedHypervolume = new NormalizedHypervolume(referenceFront).compute(objectives);
    return String.format(Locale.ROOT, "EP = %.4f, NHV = %.4f", epsilon, normalizedHypervolume);
  }
}
