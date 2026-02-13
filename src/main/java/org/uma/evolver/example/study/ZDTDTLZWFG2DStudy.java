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
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6_2D;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7_2D;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Runner to execute NSGA-II configuration validation experiments. Compares three NSGA-II variants
 * (Standard, RE3D, RE3D-Est) across ZDT, DTLZ, and WFG 2-objective benchmark problems. 
 * Generates quality indicators (Epsilon, Hypervolume, IGD, IGD+) and statistical analysis.
 *
 * <p>Configuration: PopSize 100, 10000 evaluations, 25 independent runs per configuration.
 * <p>Output: Quality indicators, LaTeX tables, Wilcoxon and Friedman statistical tests.
 */
public class ZDTDTLZWFG2DStudy {

  private static final int INDEPENDENT_RUNS = 25;
  private static final int MAX_EVALUATIONS = 20000;
  private static final int POPULATION_SIZE = 100;
  private static final String YAML_FILE = "NSGAIIDoubleFull.yaml";

  public static void main(String[] args) throws IOException {
    String experimentBaseDirectory = "results/swevo/experiments"; // Dedicated folder

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    // ZDT 2-objective problems (5 problems)
    problemList.add(new ExperimentProblem<>(new ZDT1()).setReferenceFront("ZDT1.csv"));
    problemList.add(new ExperimentProblem<>(new ZDT2()).setReferenceFront("ZDT2.csv"));
    problemList.add(new ExperimentProblem<>(new ZDT3()).setReferenceFront("ZDT3.csv"));
    problemList.add(new ExperimentProblem<>(new ZDT4()).setReferenceFront("ZDT4.csv"));
    problemList.add(new ExperimentProblem<>(new ZDT6()).setReferenceFront("ZDT6.csv"));

    // DTLZ 2-objective problems (7 problems)
    problemList.add(new ExperimentProblem<>(new DTLZ1_2D()).setReferenceFront("DTLZ1.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ2_2D()).setReferenceFront("DTLZ2.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ3_2D()).setReferenceFront("DTLZ3.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ4_2D()).setReferenceFront("DTLZ4.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ5_2D()).setReferenceFront("DTLZ5.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ6_2D()).setReferenceFront("DTLZ6.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ7_2D()).setReferenceFront("DTLZ7.2D.csv"));

    // WFG 2-objective problems (9 problems)
    problemList.add(new ExperimentProblem<>(new WFG1()).setReferenceFront("WFG1.csv"));
    problemList.add(new ExperimentProblem<>(new WFG2()).setReferenceFront("WFG2.csv"));
    problemList.add(new ExperimentProblem<>(new WFG3()).setReferenceFront("WFG3.csv"));
    problemList.add(new ExperimentProblem<>(new WFG4()).setReferenceFront("WFG4.csv"));
    problemList.add(new ExperimentProblem<>(new WFG5()).setReferenceFront("WFG5.csv"));
    problemList.add(new ExperimentProblem<>(new WFG6()).setReferenceFront("WFG6.csv"));
    problemList.add(new ExperimentProblem<>(new WFG7()).setReferenceFront("WFG7.csv"));
    problemList.add(new ExperimentProblem<>(new WFG8()).setReferenceFront("WFG8.csv"));
    problemList.add(new ExperimentProblem<>(new WFG9()).setReferenceFront("WFG9.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ZDTDTLZWFG2D")
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
    System.out.println("Quality indicators and statistical analysis complete. Data stored in: " + experimentBaseDirectory);
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
    // - BLX-alpha crossover, Lévy flight mutation, random selection
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
