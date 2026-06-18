package org.uma.evolver.example.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.multiobjective.re.*;
import org.uma.jmetal.problem.multiobjective.rwa.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class REMOEADStudy {

  private static final int INDEPENDENT_RUNS = 30;
  private static final int MAX_EVALUATIONS = 10000;
  private static final int POPULATION_SIZE = 100;
  private static final String YAML_FILE = "MOEADDouble.yaml";

  public static void main(String[] args) throws IOException {
    String experimentBaseDirectory = "experimentation/validation"; // Dedicated folder

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    // RE 2D
    problemList.add(new ExperimentProblem<>(new RE21()).setReferenceFront("RE21.csv"));
    problemList.add(new ExperimentProblem<>(new RE22()).setReferenceFront("RE22.csv"));
    problemList.add(new ExperimentProblem<>(new RE23()).setReferenceFront("RE23.csv"));
    problemList.add(new ExperimentProblem<>(new RE24()).setReferenceFront("RE24.csv"));
    problemList.add(new ExperimentProblem<>(new RE25()).setReferenceFront("RE25.csv"));

    // RE 3D
    problemList.add(new ExperimentProblem<>(new RE31()).setReferenceFront("RE31.csv"));
    problemList.add(new ExperimentProblem<>(new RE32()).setReferenceFront("RE32.csv"));
    problemList.add(new ExperimentProblem<>(new RE33()).setReferenceFront("RE33.csv"));
    problemList.add(new ExperimentProblem<>(new RE34()).setReferenceFront("RE34.csv"));
    problemList.add(new ExperimentProblem<>(new RE35()).setReferenceFront("RE35.csv"));
    problemList.add(new ExperimentProblem<>(new RE36()).setReferenceFront("RE36.csv"));
    problemList.add(new ExperimentProblem<>(new RE37()).setReferenceFront("RE37.csv"));

    // RE High-D
    problemList.add(new ExperimentProblem<>(new RE41()).setReferenceFront("RE41.csv"));
    problemList.add(new ExperimentProblem<>(new RE42()).setReferenceFront("RE42.csv"));
    problemList.add(new ExperimentProblem<>(new RE61()).setReferenceFront("RE61.csv"));
    problemList.add(new ExperimentProblem<>(new RE91()).setReferenceFront("RE91.csv"));

    // RWA Problems
    problemList.add(new ExperimentProblem<>(new RWA1()).setReferenceFront("RWA1.csv"));
    problemList.add(new ExperimentProblem<>(new RWA2()).setReferenceFront("RWA2.csv"));
    problemList.add(new ExperimentProblem<>(new RWA3()).setReferenceFront("RWA3.csv"));
    problemList.add(new ExperimentProblem<>(new RWA4()).setReferenceFront("RWA4.csv"));
    problemList.add(new ExperimentProblem<>(new RWA5()).setReferenceFront("RWA5.csv"));
    problemList.add(new ExperimentProblem<>(new RWA6()).setReferenceFront("RWA6.csv"));
    problemList.add(new ExperimentProblem<>(new RWA7()).setReferenceFront("RWA7.csv"));
    problemList.add(new ExperimentProblem<>(new RWA8()).setReferenceFront("RWA8.csv"));
    problemList.add(new ExperimentProblem<>(new RWA9()).setReferenceFront("RWA9.csv"));
    problemList.add(new ExperimentProblem<>(new RWA10()).setReferenceFront("RWA10.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("RERMOEADStudy")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(
                Arrays.asList(
                    new Epsilon(),
                    new PISAHypervolume(),
                    new InvertedGenerationalDistance(),
                    new InvertedGenerationalDistancePlus()))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(8)
            .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanTestTables<>(experiment).run();
    System.out.println("Visualization runs complete. Data stored in: " + experimentBaseDirectory);
  }

  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();

    // Standard MOEA/D configuration
    String stdMOEADConfig =
        String.join(
            " ",
            "--neighborhoodSize 20",
            "--maximumNumberOfReplacedSolutions 2",
            "--aggregationFunction penaltyBoundaryIntersection",
            "--normalizeObjectives false",
            "--pbiTheta 5.0",
            "--algorithmResult population",
            "--createInitialSolutions default",
            "--subProblemIdGenerator randomPermutationCycle",
            "--variation crossoverAndMutationVariation",
            "--crossover SBX",
            "--crossoverProbability 0.9",
            "--crossoverRepairStrategy bounds",
            "--sbxDistributionIndex 20.0",
            "--mutation polynomial",
            "--mutationProbabilityFactor 1.0",
            "--mutationRepairStrategy bounds",
            "--polynomialMutationDistributionIndex 20.0",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.9");

    // MOEA/D-DE: classical DE variant from LZ09F2 configuration
    String moeadDEConfig =
        String.join(
            " ",
            "--neighborhoodSize 20",
            "--maximumNumberOfReplacedSolutions 2",
            "--aggregationFunction tschebyscheff",
            "--normalizeObjectives true",
            "--epsilonParameterForNormalization 4",
            "--algorithmResult population",
            "--createInitialSolutions default",
            "--subProblemIdGenerator randomPermutationCycle",
            "--variation differentialEvolutionVariation",
            "--differentialEvolutionCrossover RAND_1_BIN",
            "--CR 1.0",
            "--F 0.5",
            "--mutation polynomial",
            "--mutationProbabilityFactor 1.0",
            "--mutationRepairStrategy bounds",
            "--polynomialMutationDistributionIndex 20.0",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.9");

    // MOEA/D-RE3D: best HV configuration from meta-optimization on RE3D training set
    // (Evaluation=2000, SolutionId=6, HVMinus=-0.8522044382197695)
    String moeadRE3DConfig =
        String.join(
            " ",
            "--neighborhoodSize 10",
            "--maximumNumberOfReplacedSolutions 4",
            "--aggregationFunction augmentedTschebyscheff",
            "--normalizeObjectives true",
            "--epsilonParameterForNormalization 0.3554846276496209",
            "--algorithmResult externalArchive",
            "--archiveType unboundedArchive",
            "--subProblemIdGenerator cyclicIntegerSequence",
            "--createInitialSolutions cauchy",
            "--variation differentialEvolutionVariation",
            "--differentialEvolutionCrossover RAND_2_BIN",
            "--CR 0.39469219184051985",
            "--F 0.9889020509346556",
            "--mutation uniform",
            "--mutationProbabilityFactor 0.16186713516181672",
            "--mutationRepairStrategy bounds",
            "--uniformMutationPerturbation 0.7718291417423131",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.6324336885795143");

    YAMLParameterSpace moeadParameterSpace =
        new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {

        algorithms.add(
            createMOEADAlgo(
                expProblem, run, "stdMOEAD", stdMOEADConfig, POPULATION_SIZE, moeadParameterSpace));

        algorithms.add(
            createMOEADAlgo(
                expProblem, run, "MOEADDE", moeadDEConfig, POPULATION_SIZE, moeadParameterSpace));

        algorithms.add(
            createMOEADAlgo(
                expProblem, run, "MOEADRE3D", moeadRE3DConfig, POPULATION_SIZE, moeadParameterSpace));
      }
    }
    return algorithms;
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createMOEADAlgo(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      String tag,
      String params,
      int popSize,
      YAMLParameterSpace parameterSpace) {

    DoubleMOEAD factory =
        new DoubleMOEAD(
            expProblem.getProblem(),
            popSize,
            MAX_EVALUATIONS,
            "resources/weightVectors",
            parameterSpace);
    factory.parse(params.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = factory.build();
    return new ExperimentAlgorithm<>(algorithm, tag, expProblem, run);
  }
}
