package org.uma.evolver.analysis.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateBoxplotsWithR;
import org.uma.jmetal.lab.experiment.component.impl.GenerateFriedmanTestTables;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateWilcoxonTestTablesWithR;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.multiobjective.re.*;
import org.uma.jmetal.problem.multiobjective.rwa.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Experimental study to compare NSGA-II configurations on all RE and RWA
 * problems.
 * Compares:
 * 1. NSGA-II Standard
 * 2. NSGA-II (Best RE3D Tuned)
 * 3. NSGA-II (Best RE3D_estimated Tuned)
 */
public class REStudy {

  private static final int INDEPENDENT_RUNS = 25;
  private static final int MAX_EVALUATIONS = 10000;
  private static final int POPULATION_SIZE = 100;
  private static final String YAML_FILE = "NSGAIIDoubleFull.yaml";

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

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

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList = configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment = new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>(
        "REStudy")
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
    new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(3).setDisplayNotch().run();
  }

  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();

    // Parameter Strings
    // Standard
    String pStandard = "--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2";

    // RE3D Best (Corrected from analysis)
    String pRE3D = "--algorithmResult externalArchive --populationSizeWithArchive 147 --archiveType unboundedArchive --createInitialSolutions gridBased --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover blxAlpha --crossoverProbability 0.6485460321233425 --crossoverRepairStrategy bounds --blxAlphaCrossoverAlpha 0.8632413529094627 --mutation uniform --mutationProbabilityFactor 0.8395270667276914 --mutationRepairStrategy round --uniformMutationPerturbation 0.9022056831950059 --selection tournament --selectionTournamentSize 8";

    // RE3D Estimated Best
    String pEst = "--algorithmResult externalArchive --populationSizeWithArchive 68 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 400 --variation crossoverAndMutationVariation --crossover blxAlpha --crossoverProbability 0.9080706427288077 --crossoverRepairStrategy bounds --blxAlphaCrossoverAlpha 0.8575338178558894 --mutation levyFlight --mutationProbabilityFactor 0.8920195868917605 --mutationRepairStrategy random --levyFlightMutationBeta 1.227144043359349 --levyFlightMutationStepSize 0.6805871241428736 --selection random";

    YAMLParameterSpace parameterSpace = new YAMLParameterSpace(YAML_FILE, new DoubleParameterFactory());

    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> expProblem : problemList) {

        // 1. Standard
        algorithms.add(
            createAlgo(
                expProblem, run, "NSGAII-Standard", pStandard, POPULATION_SIZE, parameterSpace));

        // 2. RE3D
        algorithms.add(
            createAlgo(expProblem, run, "NSGAII-RE3D", pRE3D, POPULATION_SIZE, parameterSpace));

        // 3. RE3D Est
        algorithms.add(
            createAlgo(expProblem, run, "NSGAII-RE3D-Est", pEst, POPULATION_SIZE, parameterSpace));
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

    DoubleNSGAII factory = new DoubleNSGAII(expProblem.getProblem(), popSize, MAX_EVALUATIONS, parameterSpace);
    factory.parse(params.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = factory.build();
    return new ExperimentAlgorithm<>(algorithm, tag, expProblem, run);
  }
}
