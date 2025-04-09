package org.uma.evolver.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateFriedmanHolmTestTables;
import org.uma.jmetal.lab.experiment.component.impl.GenerateHtmlPages;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateWilcoxonTestTablesWithR;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.lab.visualization.StudyVisualizer;
import org.uma.jmetal.problem.Problem;
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
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

public class Meta2023WFG {

  private static final int INDEPENDENT_RUNS = 25;

  public static void main(String[] args) throws IOException {
    Check.that(args.length == 1, "Missing argument: experimentBaseDirectory");

    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    problemList.add(new ExperimentProblem<>(new WFG1()).setReferenceFront("WFG1.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG2()).setReferenceFront("WFG2.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG3()).setReferenceFront("WFG3.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG4()).setReferenceFront("WFG4.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG5()).setReferenceFront("WFG5.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG6()).setReferenceFront("WFG6.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG7()).setReferenceFront("WFG7.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG8()).setReferenceFront("WFG8.2D.csv"));
    problemList.add(new ExperimentProblem<>(new WFG9()).setReferenceFront("WFG9.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ1_2D()).setReferenceFront("DTLZ1.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ2_2D()).setReferenceFront("DTLZ2.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ3_2D()).setReferenceFront("DTLZ3.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ4_2D()).setReferenceFront("DTLZ4.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ5_2D()).setReferenceFront("DTLZ5.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ6_2D()).setReferenceFront("DTLZ6.2D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ7_2D()).setReferenceFront("DTLZ7.2D.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("meta2023-wfg")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setReferenceFrontDirectory("resources/referenceFronts")
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(Arrays.asList(
                new Epsilon(),
                new PISAHypervolume(),
                new NormalizedHypervolume(),
                new InvertedGenerationalDistancePlus()
            ))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(8)
            .build();

    new ExecuteAlgorithms<>(experiment).run();

    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanHolmTestTables<>(experiment).run();
    //new GenerateFriedmanTestTables<>(experiment).run();
    //new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(3).setDisplayNotch().run();
    new GenerateHtmlPages<>(experiment, StudyVisualizer.TYPE_OF_FRONT_TO_SHOW.MEDIAN).run();
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
      List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (ExperimentProblem<DoubleSolution> experimentProblem : problemList) {
        nsgaII(algorithms, run, experimentProblem);
        smpso(algorithms, run, experimentProblem);
        EvNsgaII(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }

  private static void nsgaII(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName " + experimentProblem.getReferenceFront() + " "
            + "--algorithmResult population "
            + "--offspringPopulationSize 100 "
            + "--createInitialSolutions random "
            + "--variation crossoverAndMutationVariation "
            + "--selection tournament "
            + "--selectionTournamentSize 2 "
            + "--rankingForSelection dominanceRanking "
            + "--densityEstimatorForSelection crowdingDistance "
            + "--crossover SBX "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy bounds "
            + "--sbxDistributionIndex 20.0 "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy bounds "
            + "--polynomialMutationDistributionIndex 20.0 ")
            .split("\\s+");

    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;
    ConfigurableNSGAII autoNSGAII = new ConfigurableNSGAII(
        (DoubleProblem) experimentProblem.getProblem(), populationSize, maximumNumberOfEvaluations);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.build();

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm, "NSGAII", experimentProblem, run));
  }

  private static void smpso(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName " + experimentProblem.getReferenceFront() + " "
                + "--swarmSize 100 "
            + "--algorithmResult leaderArchive "
            + "--archiveSize 100 "
            + "--swarmInitialization random "
            + "--velocityInitialization defaultVelocityInitialization "
            + "--leaderArchive crowdingDistanceArchive "
            + "--localBestInitialization defaultLocalBestInitialization "
            + "--globalBestInitialization defaultGlobalBestInitialization "
            + "--globalBestSelection tournament "
            + "--selectionTournamentSize 2 "
            + "--perturbation frequencySelectionMutationBasedPerturbation "
            + "--frequencyOfApplicationOfMutationOperator 7 "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy bounds "
            + "--polynomialMutationDistributionIndex 20.0 "
            + "--positionUpdate defaultPositionUpdate "
            + "--velocityChangeWhenLowerLimitIsReached -1.0 "
            + "--velocityChangeWhenUpperLimitIsReached -1.0 "
            + "--globalBestUpdate defaultGlobalBestUpdate "
            + "--localBestUpdate defaultLocalBestUpdate "
            + "--velocityUpdate constrainedVelocityUpdate "
            + "--inertiaWeightComputingStrategy constantValue "
            + "--c1Min 1.5 "
            + "--c1Max 2.5 "
            + "--c2Min 1.5 "
            + "--c2Max 2.5 "
            + "--weight 0.1 ")
            .split("\\s+");

    int swarmSize = 100;
    int maximumNumberOfEvaluations = 25000;
    ConfigurableMOPSO autoMOPSO = new ConfigurableMOPSO(
        (DoubleProblem) experimentProblem.getProblem(), swarmSize, maximumNumberOfEvaluations);
    autoMOPSO.parse(parameters);

    ParticleSwarmOptimizationAlgorithm algorithm = autoMOPSO.build();

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm, "SMPSO", experimentProblem, run));
  }

  private static void EvNsgaII(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 179 --externalArchive crowdingDistanceArchive --createInitialSolutions random --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.4516361182972551 --crossoverRepairStrategy bounds --sbxDistributionIndex 334.1278555494533 --blxAlphaCrossoverAlphaValue 0.7007829446362449 --mutation nonUniform --mutationProbabilityFactor 0.4990374331433836 --mutationRepairStrategy round --polynomialMutationDistributionIndex 27.648145410058294 --linkedPolynomialMutationDistributionIndex 377.523579097924 --uniformMutationPerturbation 0.51257742340875 --nonUniformMutationPerturbation 0.8127533569986545 --selection tournament --selectionTournamentSize 9 \n ")
            .split("\\s+");

    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;
    ConfigurableNSGAII autoNSGAII = new ConfigurableNSGAII(
            (DoubleProblem) experimentProblem.getProblem(), populationSize, maximumNumberOfEvaluations);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.build();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "EvNSGAII", experimentProblem, run));
  }
}
