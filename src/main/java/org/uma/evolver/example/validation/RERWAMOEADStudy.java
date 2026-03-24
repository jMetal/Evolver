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

public class RERWAMOEADStudy {

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
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("RERWAMOEADStudy")
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
    String standardMOEADConfig =
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
            "--crossoverProbability 0.9",
            "--crossoverRepairStrategy bounds",
            "--mutation polynomial",
            "--mutationProbabilityFactor 1.0",
            "--mutationRepairStrategy bounds",
            "--polynomialMutationDistributionIndex 20.0",
            "--crossover SBX",
            "--sbxDistributionIndex 20.0",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.9");

    // Standard MOEA/D configuration WITH external archive
    String standardMOEADArchiveConfig =
        String.join(
            " ",
            "--neighborhoodSize 20",
            "--maximumNumberOfReplacedSolutions 2",
            "--aggregationFunction penaltyBoundaryIntersection",
            "--normalizeObjectives false",
            "--pbiTheta 5.0",
            "--algorithmResult externalArchive",
            "--populationSizeWithArchive 100",
            "--archiveType unboundedArchive",
            "--createInitialSolutions default",
            "--subProblemIdGenerator randomPermutationCycle",
            "--variation crossoverAndMutationVariation",
            "--crossoverProbability 0.9",
            "--crossoverRepairStrategy bounds",
            "--mutation polynomial",
            "--mutationProbabilityFactor 1.0",
            "--mutationRepairStrategy bounds",
            "--polynomialMutationDistributionIndex 20.0",
            "--crossover SBX",
            "--sbxDistributionIndex 20.0",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.9");

    // MOEA/D configuration with NSGAII-RE3D operators
    // Init: cauchy, Crossover: laplace, Mutation: levyFlight
    String moeadRE3DConfig =
        String.join(
            " ",
            "--neighborhoodSize 20",
            "--maximumNumberOfReplacedSolutions 2",
            "--aggregationFunction penaltyBoundaryIntersection",
            "--normalizeObjectives false",
            "--pbiTheta 5.0",
            "--algorithmResult externalArchive",
            "--populationSizeWithArchive 100",
            "--archiveType unboundedArchive",
            "--createInitialSolutions cauchy",
            "--subProblemIdGenerator randomPermutationCycle",
            "--variation crossoverAndMutationVariation",
            "--crossover laplace",
            "--crossoverProbability 0.7743480372",
            "--crossoverRepairStrategy bounds",
            "--laplaceCrossoverScale 0.4995269531",
            "--mutation levyFlight",
            "--mutationProbabilityFactor 1.0823942293",
            "--mutationRepairStrategy bounds",
            "--levyFlightMutationBeta 1.5821613074",
            "--levyFlightMutationStepSize 0.8210163797",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.9");

    // MOEA/D configuration with NSGAII-RWA3D operators
    // Init: cauchy, Crossover: blxAlphaBeta, Mutation: powerLaw
    String moeadRWA3DConfig =
        String.join(
            " ",
            "--neighborhoodSize 20",
            "--maximumNumberOfReplacedSolutions 2",
            "--aggregationFunction penaltyBoundaryIntersection",
            "--normalizeObjectives false",
            "--pbiTheta 5.0",
            "--algorithmResult externalArchive",
            "--populationSizeWithArchive 100",
            "--archiveType unboundedArchive",
            "--createInitialSolutions cauchy",
            "--subProblemIdGenerator randomPermutationCycle",
            "--variation crossoverAndMutationVariation",
            "--crossover blxAlphaBeta",
            "--crossoverProbability 0.4324193706",
            "--crossoverRepairStrategy round",
            "--blxAlphaBetaCrossoverAlpha 0.8993357785",
            "--blxAlphaBetaCrossoverBeta 0.7105298828",
            "--mutation powerLaw",
            "--mutationProbabilityFactor 1.7120696450",
            "--mutationRepairStrategy bounds",
            "--powerLawMutationDelta 8.8732970103",
            "--selection populationAndNeighborhoodMatingPoolSelection",
            "--neighborhoodSelectionProbability 0.9");

    YAMLParameterSpace moeadParameterSpace =
        new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {

        algorithms.add(
            createMOEADAlgo(
                expProblem,
                run,
                "MOEAD-Std",
                standardMOEADConfig,
                POPULATION_SIZE,
                moeadParameterSpace));

        algorithms.add(
            createMOEADAlgo(
                expProblem,
                run,
                "MOEAD-StdArch",
                standardMOEADArchiveConfig,
                POPULATION_SIZE,
                moeadParameterSpace));

        algorithms.add(
            createMOEADAlgo(
                expProblem, run, "MOEAD-RE3D", moeadRE3DConfig, POPULATION_SIZE, moeadParameterSpace));

        algorithms.add(
            createMOEADAlgo(
                expProblem, run, "MOEAD-RWA", moeadRWA3DConfig, POPULATION_SIZE, moeadParameterSpace));
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
