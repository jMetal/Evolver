package org.uma.evolver.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.lz09.*;
import org.uma.jmetal.problem.multiobjective.uf.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

public class MOEADLZ09 {

  private static final int INDEPENDENT_RUNS = 30;
  private static final int MAXIMUM_NUMBER_OF_EVALUATIONS = 175000;

  private static final String WEIGHT_VECTOR_DIRECTORY = "resources/weightVectors" ;

  public static void main(String[] args) throws IOException {
    Check.that(args.length == 1, "Missing argument: experimentBaseDirectory");

    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    problemList.add(new ExperimentProblem<>(new LZ09F1()).setReferenceFront("LZ09_F1.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F2()).setReferenceFront("LZ09_F2.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F3()).setReferenceFront("LZ09_F3.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F4()).setReferenceFront("LZ09_F4.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F5()).setReferenceFront("LZ09_F5.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F6()).setReferenceFront("LZ09_F6.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F7()).setReferenceFront("LZ09_F7.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F8()).setReferenceFront("LZ09_F8.csv"));
    problemList.add(new ExperimentProblem<>(new LZ09F9()).setReferenceFront("LZ09_F9.csv"));
    problemList.add((new ExperimentProblem<>(new UF1())));
    problemList.add((new ExperimentProblem<>(new UF2())));
    problemList.add((new ExperimentProblem<>(new UF3())));
    problemList.add((new ExperimentProblem<>(new UF4())));
    problemList.add((new ExperimentProblem<>(new UF5())));
    problemList.add((new ExperimentProblem<>(new UF6())));
    problemList.add((new ExperimentProblem<>(new UF7())));
    problemList.add((new ExperimentProblem<>(new UF8())));
    problemList.add((new ExperimentProblem<>(new UF9())));
    problemList.add((new ExperimentProblem<>(new UF10())));


    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("MOEADLZ09GECCO-2-7-9-2025")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setReferenceFrontDirectory("resources/referenceFrontsCSV")
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

    //new ExecuteAlgorithms<>(experiment).run();

    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateFriedmanHolmTestTables<>(experiment).run();
    //new GenerateFriedmanTestTables<>(experiment).run();
    //new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(3).setDisplayNotch().run();
    //new GenerateHtmlPages<>(experiment, StudyVisualizer.TYPE_OF_FRONT_TO_SHOW.MEDIAN).run();
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
        moead(algorithms, run, experimentProblem);
        moeadde(algorithms, run, experimentProblem);
        evmoead(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }

  private static void moead(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName " + experimentProblem.getReferenceFront() + " "
            + "--neighborhoodSize 20"
            + " --maximumNumberOfReplacedSolutions 2 "
            + "--aggregationFunction penaltyBoundaryIntersection "
            + "--pbiTheta 5.0 "
            + "--sequenceGenerator permutation "
            + "--normalizeObjectives False "
            + "--algorithmResult population "
            + "--createInitialSolutions random "
            + "--variation crossoverAndMutationVariation "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy random "
            + "--polynomialMutationDistributionIndex 20.0 "
            + "--crossover  SBX "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy random "
            + "--sbxDistributionIndex 20.0 "
            + "--selection populationAndNeighborhoodMatingPoolSelection "
            + "--neighborhoodSelectionProbability 0.9 \n")
            .split("\\s+");

    int populationSize = 100;
    ConfigurableMOEAD moead = new ConfigurableMOEAD(
        (DoubleProblem) experimentProblem.getProblem(), populationSize, MAXIMUM_NUMBER_OF_EVALUATIONS, WEIGHT_VECTOR_DIRECTORY);
    moead.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm, "MOEAD", experimentProblem, run));
  }

  private static void moeadde(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
            ("--referenceFrontFileName " + experimentProblem.getReferenceFront() + " "
                    + "--neighborhoodSize 20"
                    + " --maximumNumberOfReplacedSolutions 2 "
                    + "--aggregationFunction tschebyscheff "
                    + "--normalizeObjectives False "
                    + "--sequenceGenerator permutation "
                    + "--algorithmResult population "
                    + "--createInitialSolutions random "
                    + "--variation differentialEvolutionVariation "
                    + "--mutation polynomial "
                    + "--mutationProbabilityFactor 1.0 "
                    + "--mutationRepairStrategy random "
                    + "--polynomialMutationDistributionIndex 20.0 "
                    + "--selection populationAndNeighborhoodMatingPoolSelection "
                    + "--differentialEvolutionCrossover RAND_1_BIN "
                    + "--CR 1.0 "
                    + "--F 0.5 "
                    + "--neighborhoodSelectionProbability 0.9 \n")
                    .split("\\s+");

    int populationSize = 100;
    ConfigurableMOEAD moead = new ConfigurableMOEAD(
            (DoubleProblem) experimentProblem.getProblem(), populationSize, MAXIMUM_NUMBER_OF_EVALUATIONS, WEIGHT_VECTOR_DIRECTORY);
    moead.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "MOEAD-DE", experimentProblem, run));
  }

  private static void evmoead(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--neighborhoodSize 20 --maximumNumberOfReplacedSolutions 3 --aggregationFunction tschebyscheff --normalizeObjectives True --epsilonParameterForNormalizing 1.0173142889333502 --pbiTheta 158.4813181794043 --sequenceGenerator integerSequence --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.26948068961451727 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 28.240701251515937 --linkedPolynomialMutationDistributionIndex 222.08033048650782 --uniformMutationPerturbation 0.4558597275173921 --nonUniformMutationPerturbation 0.2239303081543158 --crossover BLX_ALPHA --crossoverProbability 0.9213553972423311 --crossoverRepairStrategy random --sbxDistributionIndex 44.073441743986095 --blxAlphaCrossoverAlphaValue 0.15639673821508548 --differentialEvolutionCrossover RAND_1_EXP --CR 0.9671368690347983 --F 0.6287597610846296 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.9070189421470535")
            .split("\\s+");

    int populationSize = 100;
    ConfigurableMOEAD moead = new ConfigurableMOEAD(
            (DoubleProblem) experimentProblem.getProblem(), populationSize, MAXIMUM_NUMBER_OF_EVALUATIONS, WEIGHT_VECTOR_DIRECTORY);
    moead.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "EvMOEAD", experimentProblem, run));
  }
}
