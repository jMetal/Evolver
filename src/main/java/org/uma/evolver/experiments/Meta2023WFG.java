package org.uma.evolver.experiments;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.lab.visualization.StudyVisualizer;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.qualityindicator.impl.*;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Meta2023WFG {
  private static final int INDEPENDENT_RUNS = 25;

  public static void main(String[] args) throws IOException {
    Check.that(args.length == 1, "Missing argument: experimentBaseDirectory");

    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    //problemList.add(new ExperimentProblem<>(new ZDT1()).setReferenceFront("ZDT1.csv"));
    //problemList.add(new ExperimentProblem<>(new ZDT2()).setReferenceFront("ZDT2.csv"));
    //problemList.add(new ExperimentProblem<>(new ZDT3()).setReferenceFront("ZDT3.csv"));
    //problemList.add(new ExperimentProblem<>(new ZDT4()).setReferenceFront("ZDT4.csv"));
    //problemList.add(new ExperimentProblem<>(new ZDT6()).setReferenceFront("ZDT6.csv"));

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
    //problemList.add(new ExperimentProblem<>(new RE21()).setReferenceFront("RE21.csv"));
    //problemList.add(new ExperimentProblem<>(new RE22()).setReferenceFront("RE22.csv"));
    //problemList.add(new ExperimentProblem<>(new RE23()).setReferenceFront("RE23.csv"));
    //problemList.add(new ExperimentProblem<>(new RE24()).setReferenceFront("RE24.csv"));
    //problemList.add(new ExperimentProblem<>(new RE25()).setReferenceFront("RE25.csv"));
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
                //new Spread(),
                //new GenerationalDistance(),
                new PISAHypervolume(),
                new NormalizedHypervolume(),
                //new InvertedGenerationalDistance(),
                new InvertedGenerationalDistancePlus()
                            ))
            .setIndependentRuns(INDEPENDENT_RUNS)
            .setNumberOfCores(8)
            .build();

    //new ExecuteAlgorithms<>(experiment).run();

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
        //omopso(algorithms, run, experimentProblem);
        //autoNSGAII_ZDT_1(algorithms, run, experimentProblem);
        //autoNSGAII_ZDT_2(algorithms, run, experimentProblem);
        //autoNSGAII_WFG_1(algorithms, run, experimentProblem);
        //autoNSGAII_WFG_2(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }

  private static void nsgaII(List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run, ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName "+ experimentProblem.getReferenceFront() + " "
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

    int populationSize = 100 ;
    int maximumNumberOfEvaluations = 25000 ;
    ConfigurableNSGAII autoNSGAII = new ConfigurableNSGAII(
        (DoubleProblem) experimentProblem.getProblem(), populationSize, maximumNumberOfEvaluations);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.build() ;

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm, "NSGAII", experimentProblem, run));
  }

  private static void smpso(List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run, ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName "+ experimentProblem.getReferenceFront() + " "
            + "--maximumNumberOfEvaluations 25000 "
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

    int swarmSize = 100 ;
    int maximumNumberOfEvaluations = 25000 ;
    ConfigurableMOPSO autoMOPSO = new ConfigurableMOPSO((DoubleProblem) experimentProblem.getProblem(), swarmSize, maximumNumberOfEvaluations);
    autoMOPSO.parse(parameters);

    ParticleSwarmOptimizationAlgorithm algorithm = autoMOPSO.build();

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm, "SMPSO", experimentProblem, run));
  }

  /*
  private static void autoNSGAII_ZDT_1(List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run, ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
            ("--problemName "+  experimentProblem.getProblem().getClass().getName() + " "
                    + "--randomGeneratorSeed 12 "
                    + "--referenceFrontFileName "+ experimentProblem.getReferenceFront() + " "
                    + "--maximumNumberOfEvaluations 25000 "
                    + "--populationSize 100 "
                    + "--algorithmResult externalArchive --populationSizeWithArchive 11 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --variation crossoverAndMutationVariation --offspringPopulationSize 1 --crossover BLX_ALPHA --crossoverProbability 0.9578353694237023 --crossoverRepairStrategy bounds --sbxDistributionIndex 48.07653061562292 --blxAlphaCrossoverAlphaValue 0.9882974166436457 --mutation nonUniform --mutationProbabilityFactor 0.2782737712637122 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 336.38698644162173 --linkedPolynomialMutationDistributionIndex 353.64452343752856 --uniformMutationPerturbation 0.46713765988525724 --nonUniformMutationPerturbation 0.1685402491936708 --selection random --selectionTournamentSize 8")
                    .split("\\s+");

    AutoNSGAII autoNSGAII = new AutoNSGAII();
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.create();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "NSGAIIZDT1", experimentProblem, run));
  }

  private static void autoNSGAII_ZDT_2(List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run, ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
            ("--problemName "+  experimentProblem.getProblem().getClass().getName() + " "
                    + "--randomGeneratorSeed 12 "
                    + "--referenceFrontFileName "+ experimentProblem.getReferenceFront() + " "
                    + "--maximumNumberOfEvaluations 25000 "
                    + "--populationSize 100 "
                    + "--algorithmResult externalArchive --populationSizeWithArchive 59 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --variation crossoverAndMutationVariation --offspringPopulationSize 33 --crossover BLX_ALPHA --crossoverProbability 0.918599648329018 --crossoverRepairStrategy bounds --sbxDistributionIndex 255.5481629509218 --blxAlphaCrossoverAlphaValue 0.9588110901297404 --mutation uniform --mutationProbabilityFactor 0.2638152312022614 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 173.0476180367325 --linkedPolynomialMutationDistributionIndex 275.55993374625064 --uniformMutationPerturbation 0.9966543684035646 --nonUniformMutationPerturbation 0.6171872286467884 --selection tournament --selectionTournamentSize 7 ")
                    .split("\\s+");

    AutoNSGAII autoNSGAII = new AutoNSGAII();
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.create();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "NSGAIIZDT2", experimentProblem, run));
  }

  private static void autoNSGAII_WFG_1(List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run, ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
            ("--problemName "+  experimentProblem.getProblem().getClass().getName() + " "
                    + "--randomGeneratorSeed 12 "
                    + "--referenceFrontFileName "+ experimentProblem.getReferenceFront() + " "
                    + "--maximumNumberOfEvaluations 25000 "
                    + "--populationSize 100 "
                    + "--algorithmResult externalArchive --populationSizeWithArchive 43 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --variation crossoverAndMutationVariation --offspringPopulationSize 35 --crossover BLX_ALPHA --crossoverProbability 0.8588994839659011 --crossoverRepairStrategy bounds --sbxDistributionIndex 319.1430042013738 --blxAlphaCrossoverAlphaValue 0.5476843009597798 --mutation uniform --mutationProbabilityFactor 0.15403396699778324 --mutationRepairStrategy random --polynomialMutationDistributionIndex 225.42984139127458 --linkedPolynomialMutationDistributionIndex 10.000688548280865 --uniformMutationPerturbation 0.26940662703575535 --nonUniformMutationPerturbation 0.478093398311044 --selection random --selectionTournamentSize 9 ")
                    .split("\\s+");

    AutoNSGAII autoNSGAII = new AutoNSGAII();
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.create();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "NSGAIIWFG1", experimentProblem, run));
  }

  private static void autoNSGAII_WFG_2(List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run, ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
            ("--problemName "+  experimentProblem.getProblem().getClass().getName() + " "
                    + "--randomGeneratorSeed 12 "
                    + "--referenceFrontFileName "+ experimentProblem.getReferenceFront() + " "
                    + "--maximumNumberOfEvaluations 25000 "
                    + "--populationSize 100 "
                    + "--algorithmResult externalArchive --populationSizeWithArchive 61 --externalArchive crowdingDistanceArchive --createInitialSolutions random --variation crossoverAndMutationVariation --offspringPopulationSize 68 --crossover BLX_ALPHA --crossoverProbability 0.8588994839659011 --crossoverRepairStrategy bounds --sbxDistributionIndex 285.15667392232655 --blxAlphaCrossoverAlphaValue 0.5475089042935802 --mutation linkedPolynomial --mutationProbabilityFactor 0.16107731365092728 --mutationRepairStrategy round --polynomialMutationDistributionIndex 153.5142613195875 --linkedPolynomialMutationDistributionIndex 11.335383841556112 --uniformMutationPerturbation 0.28723174417575 --nonUniformMutationPerturbation 0.6278406532530493 --selection tournament --selectionTournamentSize 4 ")
                    .split("\\s+");

    AutoNSGAII autoNSGAII = new AutoNSGAII();
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = autoNSGAII.create();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "NSGAIIWFG2", experimentProblem, run));
  }
*/
}
