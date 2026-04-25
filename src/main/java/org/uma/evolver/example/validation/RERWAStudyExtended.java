package org.uma.evolver.example.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.AGEMOEABuilder;
import org.uma.jmetal.component.algorithm.multiobjective.RVEABuilder;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.JMetalException;
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

/**
 * Runner to execute algorithm configuration validation experiments. Compares four algorithm
 * variants (NSGAII-Standard, MOEAD-Standard, NSGAII-RE3D, NSGAII-RWA3D) across RE and RWA
 * benchmark problems. Generates FUN (Pareto Fronts) and VAR (Decision Variables) files for
 * analysis.
 *
 * <p>Configuration: PopSize 100, 10000 evaluations, 25 independent runs per configuration.
 */
public class RERWAStudyExtended {

  private static final int INDEPENDENT_RUNS = 30;
  private static final int MAX_EVALUATIONS = 10000;
  private static final int POPULATION_SIZE = 100;
  private static final String YAML_FILE = "NSGAIIDouble.yaml";

  public static void main(String[] args) throws IOException {
    String experimentBaseDirectory = "experimentation/validationExtended/"; // Dedicated folder

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
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("RERWAStudyExtended")
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

    //new ExecuteAlgorithms<>(experiment).run();
    //new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanTestTables<>(experiment).run();
    System.out.println("Visualization runs complete. Data stored in: " + experimentBaseDirectory);
  }

  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();

    // Standard NSGA-II configuration (Deb et al., 2002)
    // - SBX crossover (pc=0.9, distribution index=20)
    // - Polynomial mutation (pm=1/n, distribution index=20)
    // - Binary tournament selection
    String standardNSGAIIConfig =
        String.join(
            " ",
            "--algorithmResult population",
            "--createInitialSolutions default",
            "--variation crossoverAndMutationVariation",
            "--offspringPopulationSize 100",
            "--crossover SBX",
            "--crossoverProbability 0.9",
            "--crossoverRepairStrategy bounds",
            "--sbxDistributionIndex 20.0",
            "--mutation polynomial",
            "--mutationProbabilityFactor 1.0",
            "--mutationRepairStrategy bounds",
            "--polynomialMutationDistributionIndex 20.0",
            "--selection tournament",
            "--selectionTournamentSize 2");

    // Standard MOEA/D configuration
    // - SBX crossover, polynomial mutation, PBI aggregation
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

    // Representative configuration for RE 3-objective problems.
    // Derived from 240 runs (2 datasets x 4 budgets x 30 runs):
    //   categorical params = mode; numerical params = median over runs using the modal combination.
    // Modal combination: laplace crossover (40%) + levyFlight mutation (55%)
    //                  + boltzmann selection (30%) + cauchy init (38%)
    // Median HV of all runs: 0.851404
    String bestRE3DConfig =
            String.join(
                    " ",
                    "--algorithmResult externalArchive",
                    "--archiveType unboundedArchive",
                    "--createInitialSolutions cauchy",
                    "--offspringPopulationSize 5",
                    "--variation crossoverAndMutationVariation",
                    "--crossover laplace",
                    "--crossoverProbability 0.7152172666",
                    "--crossoverRepairStrategy bounds",
                    "--laplaceCrossoverScale 0.4995269531",
                    "--mutation levyFlight",
                    "--mutationProbabilityFactor 1.0823942293",
                    "--mutationRepairStrategy bounds",
                    "--levyFlightMutationBeta 1.1471198048",
                    "--levyFlightMutationStepSize 0.6726913385",
                    "--selection boltzmann",
                    "--boltzmannTemperature 62.6939260389",
                    "--populationSizeWithArchive 38");

    // Representative configuration for RWA 3-objective problems.
    // Derived from 240 runs (2 datasets x 4 budgets x 30 runs):
    //   categorical params = mode; numerical params = median over runs using the modal combination.
    // Modal combination: blxAlphaBeta crossover (53%) + powerLaw mutation (93%)
    //                  + ranking selection (25%) + cauchy init (68%)
    // Median HV of all runs: 0.585837
    String bestRWA3DConfig =
            String.join(
                    " ",
                    "--algorithmResult externalArchive",
                    "--archiveType unboundedArchive",
                    "--createInitialSolutions cauchy",
                    "--variation crossoverAndMutationVariation",
                    "--offspringPopulationSize 10",
                    "--crossover blxAlphaBeta",
                    "--crossoverProbability 0.3104171769",
                    "--crossoverRepairStrategy bounds",
                    "--blxAlphaBetaCrossoverAlpha 0.8886521503",
                    "--blxAlphaBetaCrossoverBeta 0.9222591005",
                    "--mutation powerLaw",
                    "--mutationProbabilityFactor 1.7763566184",
                    "--mutationRepairStrategy round",
                    "--powerLawMutationDelta 9.0562569715",
                    "--selection ranking",
                    "--populationSizeWithArchive 41");

    YAMLParameterSpace parameterSpace =
        new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());
    YAMLParameterSpace moEADParameterSpace =
        new YAMLParameterSpace("MOEADDouble.yaml", new DoubleParameterFactory());

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {

        algorithms.add(
            createNSGAII(
                expProblem,
                run,
                "NSGAII-Std",
                standardNSGAIIConfig,
                POPULATION_SIZE,
                parameterSpace));

        algorithms.add(
            createMOEAD(
                expProblem,
                run,
                "MOEAD",
                standardMOEADConfig,
                POPULATION_SIZE,
                moEADParameterSpace));

        algorithms.add(
            createRVEA(
                expProblem,
                run,
                "RVEA"));

        /*
        algorithms.add(
            createAGEMOEA(
                expProblem,
                run,
                "AGE-MOEA"));
       */
        algorithms.add(
            createNSGAII(
                expProblem, run, "NSGAII-RE3D", bestRE3DConfig, POPULATION_SIZE, parameterSpace));

        algorithms.add(
            createNSGAII(
                expProblem, run, "NSGAII-RWA3D", bestRWA3DConfig, POPULATION_SIZE, parameterSpace));
      }
    }
    return algorithms;
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createMOEAD(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      String tag,
      String params,
      int popSize,
      YAMLParameterSpace parameterSpace) {

    DoubleMOEAD factory =
        new DoubleMOEAD(expProblem.getProblem(), popSize, MAX_EVALUATIONS,
            "resources/weightVectors", parameterSpace);
    factory.parse(params.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = factory.build();
    return new ExperimentAlgorithm<>(algorithm, tag, expProblem, run);
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createNSGAII(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      String tag,
      String params,
      int popSize,
      YAMLParameterSpace parameterSpace) {

    DoubleNSGAII factory =
        new DoubleNSGAII(expProblem.getProblem(), popSize, MAX_EVALUATIONS, parameterSpace);
    factory.parse(params.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = factory.build();
    return new ExperimentAlgorithm<>(algorithm, tag, expProblem, run);
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createRVEA(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      String tag) {

    Problem<DoubleSolution> problem = expProblem.getProblem();

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / problem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int numberOfObjectives = problem.numberOfObjectives();
    double[][] weightVectors;
    try {
      weightVectors = VectorUtils.readVectors("resources/weightVectors/W" + numberOfObjectives + "D_100.dat");
    } catch (IOException e) {
      throw new JMetalException("Error reading weight vectors for " + numberOfObjectives + " objectives", e);
    }

    int populationSize = weightVectors.length;
    int maxEvaluations = MAX_EVALUATIONS;
    int h = 12;
    double alpha = 2.0;
    double fr = 0.1;

    Termination termination = new TerminationByEvaluations(maxEvaluations);

    EvolutionaryAlgorithm<DoubleSolution> rvea =
        new RVEABuilder<>(
                problem, populationSize, maxEvaluations, crossover, mutation, alpha, fr, h)
            .setTermination(termination)
            .setReferenceVectors(Arrays.asList(weightVectors))
            .build();

    return new ExperimentAlgorithm<>(rvea, tag, expProblem, run);
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createAGEMOEA(
      ExperimentProblem<DoubleSolution> expProblem,
      int run,
      String tag) {

    Problem<DoubleSolution> problem = expProblem.getProblem();

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / problem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = POPULATION_SIZE;
    int offspringPopulationSize = POPULATION_SIZE;

    Termination termination = new TerminationByEvaluations(MAX_EVALUATIONS);

    EvolutionaryAlgorithm<DoubleSolution> agemoea =
        new AGEMOEABuilder<>(problem, populationSize, offspringPopulationSize, crossover, mutation, AGEMOEABuilder.Variant.AGEMOEA)
            .setTermination(termination)
            .build();

    return new ExperimentAlgorithm<>(agemoea, tag, expProblem, run);
  }
}
