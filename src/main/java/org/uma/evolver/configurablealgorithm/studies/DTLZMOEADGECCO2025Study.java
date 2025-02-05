package org.uma.evolver.configurablealgorithm.studies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.*;
import org.uma.jmetal.lab.experiment.studies.DTLZStudy;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Example of experimental study based on solving the ZDT problems with the algorithms NSGAII,
 * MOEA/D, and SMPSO.
 *
 * This experiment assumes that the reference Pareto front are known and that, given a problem named
 * P, there is a corresponding file called P.csv containing its corresponding Pareto front. If this
 * is not the case, please refer to class {@link DTLZStudy} to see an example of how to explicitly
 * indicate the name of those files.
 *
 * Five quality indicators are used for performance assessment: {@link Epsilon}, {@link Spread},
 * {@link GenerationalDistance}, {@link PISAHypervolume}, and {@link InvertedGenerationalDistancePlus}.
 *
 * The steps to carry out are:
 * 1. Configure the experiment
 * 2. Execute the algorithms
 * 3. Compute que quality indicators
 * 4. Generate Latex tables reporting means and medians, and tables with statistical tests
 * 5. Generate HTML pages with tables, boxplots, and fronts.
 *
 * @author Antonio J. Nebro
 */

public class DTLZMOEADGECCO2025Study {
  private static final int INDEPENDENT_RUNS = 30;
  private static final String weightVectorDirectory = "resources/weightVectors" ;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = List.of(
            new ExperimentProblem<>(new DTLZ1()).setReferenceFront("DTLZ1.3D.csv"),
            new ExperimentProblem<>(new DTLZ2()).setReferenceFront("DTLZ2.3D.csv"),
            new ExperimentProblem<>(new DTLZ3()).setReferenceFront("DTLZ3.3D.csv"),
            new ExperimentProblem<>(new DTLZ4()).setReferenceFront("DTLZ4.3D.csv"),
            new ExperimentProblem<>(new DTLZ5()).setReferenceFront("DTLZ5.3D.csv"),
            new ExperimentProblem<>(new DTLZ6()).setReferenceFront("DTLZ6.3D.csv"),
            new ExperimentProblem<>(new DTLZ7()).setReferenceFront("DTLZ7.3D.csv")
    );

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("DTLZMOEADGECCO2025Study")
                    .setAlgorithmList(algorithmList)
                    .setProblemList(problemList)
                    .setReferenceFrontDirectory("resources/referenceFronts")
                    .setExperimentBaseDirectory(experimentBaseDirectory)
                    .setOutputParetoFrontFileName("FUN")
                    .setOutputParetoSetFileName("VAR")
                    .setIndicatorList(List.of(
                            new Epsilon(),
                            new PISAHypervolume(),
                            new NormalizedHypervolume(),
                            new InvertedGenerationalDistancePlus()))
                    .setIndependentRuns(INDEPENDENT_RUNS)
                    .setNumberOfCores(8)
                    .build();

    new ExecuteAlgorithms<>(experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    new GenerateLatexTablesWithStatistics(experiment).run();
    new GenerateFriedmanHolmTestTables<>(experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(2).run();
    //new GenerateHtmlPages<>(experiment, StudyVisualizer.TYPE_OF_FRONT_TO_SHOW.MEDIAN).run() ;
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
          List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < INDEPENDENT_RUNS; run++) {
      for (var experimentProblem : problemList) {
        moead(algorithms, run, experimentProblem);
        moead500(algorithms, run, experimentProblem);
        moead1000(algorithms, run, experimentProblem);
        moead2000(algorithms, run, experimentProblem);
        moead3000(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }


  private static void moead(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--algorithmResult population "
                + "--offspringPopulationSize 1 "
                + "--sequenceGenerator integerSequence "
                + "--createInitialSolutions random "
                + "--normalizeObjectives false "
                + "--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + "--aggregationFunction penaltyBoundaryIntersection "
                + "--pbiTheta 5.0 "
                + "--neighborhoodSelectionProbability 0.9 "
                + "--variation crossoverAndMutationVariation "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 ")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 40000, weightVectorDirectory);

    autoMOEAD.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoMOEAD.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "MOEAD", experimentProblem, run));
  }

  private static void moead500(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--neighborhoodSize 45 --maximumNumberOfReplacedSolutions 4 --aggregationFunction modifiedTschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 24.133930757414657 --pbiTheta 126.73357735580986 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions random --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.6545620256809188 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 30.568569349876537 --linkedPolynomialMutationDistributionIndex 298.921464343801 --uniformMutationPerturbation 0.6913284759683381 --nonUniformMutationPerturbation 0.42101470058322665 --crossover BLX_ALPHA --crossoverProbability 0.48974230974582056 --crossoverRepairStrategy bounds --sbxDistributionIndex 121.45116310681917 --blxAlphaCrossoverAlphaValue 0.7833271973554256 --differentialEvolutionCrossover RAND_1_EXP --CR 0.06645844348741814 --F 0.9295069934353836 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.5294694734336065 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 40000, weightVectorDirectory);

    autoMOEAD.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoMOEAD.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "MOEAD500", experimentProblem, run));  }

  private static void moead1000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--neighborhoodSize 15 --maximumNumberOfReplacedSolutions 3 --aggregationFunction modifiedTschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 8.6084631671485 --pbiTheta 160.95448322311123 --algorithmResult externalArchive --externalArchive crowdingDistanceArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.40293325668683966 --mutationRepairStrategy random --polynomialMutationDistributionIndex 80.46395016558895 --linkedPolynomialMutationDistributionIndex 282.5543515562248 --uniformMutationPerturbation 0.2361961463462642 --nonUniformMutationPerturbation 0.9187863755050001 --crossover BLX_ALPHA --crossoverProbability 0.05610384064601298 --crossoverRepairStrategy random --sbxDistributionIndex 144.95717666451156 --blxAlphaCrossoverAlphaValue 0.10533431486386641 --differentialEvolutionCrossover RAND_1_EXP --CR 0.2486962067758639 --F 0.9516961348698919 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.8422153612940082 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 40000, weightVectorDirectory);

    autoMOEAD.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoMOEAD.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "MOEAD1000", experimentProblem, run));
  }

  private static void moead2000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--neighborhoodSize 26 --maximumNumberOfReplacedSolutions 3 --aggregationFunction modifiedTschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 19.470246644483343 --pbiTheta 97.45048480844167 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.5974074776378784 --mutationRepairStrategy round --polynomialMutationDistributionIndex 399.2294934331864 --linkedPolynomialMutationDistributionIndex 313.75859718108444 --uniformMutationPerturbation 0.29448015083521445 --nonUniformMutationPerturbation 0.7891135251700446 --crossover wholeArithmetic --crossoverProbability 0.4970944095915968 --crossoverRepairStrategy round --sbxDistributionIndex 316.35220909100843 --blxAlphaCrossoverAlphaValue 0.7158487011899302 --differentialEvolutionCrossover RAND_1_BIN --CR 0.1444344570113942 --F 0.9830320977410791 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.4739941920664111 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 40000, weightVectorDirectory);

    autoMOEAD.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoMOEAD.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "MOEAD2000", experimentProblem, run));
  }

  private static void moead3000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--neighborhoodSize 17 --maximumNumberOfReplacedSolutions 3 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 16.74427554837178 --pbiTheta 102.33621092188483 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.365757626005199 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 330.9192060953075 --linkedPolynomialMutationDistributionIndex 259.6234057548009 --uniformMutationPerturbation 0.25039687229006147 --nonUniformMutationPerturbation 0.06369128222969085 --crossover SBX --crossoverProbability 0.7718296929024707 --crossoverRepairStrategy random --sbxDistributionIndex 39.14026628165764 --blxAlphaCrossoverAlphaValue 0.9664474833035351 --differentialEvolutionCrossover RAND_1_BIN --CR 0.16954180521284054 --F 0.996915805902218 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.6398452490955031 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 40000, weightVectorDirectory);

    autoMOEAD.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoMOEAD.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "MOEAD3000", experimentProblem, run));
  }
}
