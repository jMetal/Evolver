package org.uma.evolver.example.study;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
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

/**
 * Runner to execute NSGA-II configuration validation experiments. Compares three NSGA-II variants
 * (Standard, RE3D, RE3D-Est) across RE and RWA benchmark problems. Generates FUN (Pareto Fronts)
 * and VAR (Decision Variables) files for analysis.
 *
 * <p>Configuration: PopSize 100, 10000 evaluations, 25 independent runs per configuration.
 */
public class REStudy {

  private static final int INDEPENDENT_RUNS = 25;
  private static final int MAX_EVALUATIONS = 10000;
  private static final int POPULATION_SIZE = 100;
  private static final String YAML_FILE = "NSGAIIDoubleFull.yaml";

  public static void main(String[] args) throws IOException {
    String experimentBaseDirectory = "results/swevo/experiments"; // Dedicated folder

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
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("REStudy")
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

    // new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
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

    // Best configuration found by Evolver on RE 3-objective problems (Evaluation 2000, best HV)
    // - Laplace crossover, Lévy flight mutation, Boltzmann selection
    // Source: results/swevo/nsgaii/RE3D/VAR_CONF.txt
    String bestRE3DConfig =
        String.join(
            " ",
            "--algorithmResult externalArchive",
            "--populationSizeWithArchive 13",
            "--archiveType unboundedArchive",
            "--createInitialSolutions default",
            "--offspringPopulationSize 20",
            "--variation crossoverAndMutationVariation",
            "--crossover laplace",
            "--crossoverProbability 0.8217834829790737",
            "--crossoverRepairStrategy bounds",
            "--laplaceCrossoverScale 0.4959216532897993",
            "--mutation levyFlight",
            "--mutationProbabilityFactor 0.6957867741586558",
            "--mutationRepairStrategy bounds",
            "--levyFlightMutationBeta 1.3286787674272937",
            "--levyFlightMutationStepSize 0.4842355700261636",
            "--selection boltzmann",
            "--boltzmannTemperature 60.81347202071671");

    // Estimated best configuration for RE 3-objective problems (Evaluation 2000, best HV)
    // - BLX-alpha crossover, Lévy flight mutation, stochastic universal sampling
    // Source: results/swevo/nsgaii/RE3D_estimated/VAR_CONF.txt
    String estimatedBestRE3DConfig =
        String.join(
            " ",
            "--algorithmResult externalArchive",
            "--populationSizeWithArchive 57",
            "--archiveType unboundedArchive",
            "--createInitialSolutions default",
            "--offspringPopulationSize 50",
            "--variation crossoverAndMutationVariation",
            "--crossover blxAlpha",
            "--crossoverProbability 0.6649399143186957",
            "--crossoverRepairStrategy bounds",
            "--blxAlphaCrossoverAlpha 0.7431531584790442",
            "--mutation levyFlight",
            "--mutationProbabilityFactor 1.3932191601253432",
            "--mutationRepairStrategy bounds",
            "--levyFlightMutationBeta 1.1619688870059017",
            "--levyFlightMutationStepSize 0.9862471851423424",
            "--selection stochasticUniversalSampling");

    YAMLParameterSpace parameterSpace =
        new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {

        algorithms.add(
            createAlgo(
                expProblem,
                run,
                "NSGAII-Standard",
                standardNSGAIIConfig,
                POPULATION_SIZE,
                parameterSpace));

        algorithms.add(
            createAlgo(
                expProblem, run, "NSGAII-RE3D", bestRE3DConfig, POPULATION_SIZE, parameterSpace));

        algorithms.add(
            createAlgo(
                expProblem,
                run,
                "NSGAII-RE3D-Est",
                estimatedBestRE3DConfig,
                POPULATION_SIZE,
                parameterSpace));
      }
    }
    return algorithms;
  }

  private static ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>> createAlgo(
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
}
