package org.uma.evolver.configurablealgorithm.studies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
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
import org.uma.jmetal.problem.multiobjective.zdt.*;
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

public class ZDTMOEADGECCO2025Study {
  private static final int INDEPENDENT_RUNS = 30;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = List.of(
        new ExperimentProblem<>(new ZDT1()),
        new ExperimentProblem<>(new ZDT2()),
        new ExperimentProblem<>(new ZDT3()),
        new ExperimentProblem<>(new ZDT4()),
        new ExperimentProblem<>(new ZDT6())
    );

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ZDTMOEADGECCO2025Study")
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

    //new ExecuteAlgorithms<>(experiment).run();
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
                + "--maximumNumberOfEvaluations 25000 "
                + "--algorithmResult population "
                + "--populationSize 100 "
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

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 25000, "");

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
                + "--maximumNumberOfEvaluations 25000 "
                + "--neighborhoodSize 24 --maximumNumberOfReplacedSolutions 4 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 17.489021848407592 --pbiTheta 8.593768287193647 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions random --variation differentialEvolutionVariation --mutation linkedPolynomial --mutationProbabilityFactor 1.10705913311224 --mutationRepairStrategy round --polynomialMutationDistributionIndex 61.29734050543426 --linkedPolynomialMutationDistributionIndex 32.84447217761891 --uniformMutationPerturbation 0.6479750158583536 --nonUniformMutationPerturbation 0.9679762480204172 --crossover wholeArithmetic --crossoverProbability 0.06586375547103651 --crossoverRepairStrategy bounds --sbxDistributionIndex 220.4219744189952 --blxAlphaCrossoverAlphaValue 0.45406168387747925 --differentialEvolutionCrossover RAND_1_BIN --CR 0.3862666619773416 --F 0.2582619050169491 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.43780271603757853 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 25000, "");

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
                + "--maximumNumberOfEvaluations 25000 "
                + "--neighborhoodSize 5 --maximumNumberOfReplacedSolutions 4 --aggregationFunction modifiedTschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 22.473703699342988 --pbiTheta 159.70577351693763 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation linkedPolynomial --mutationProbabilityFactor 0.49966179308838576 --mutationRepairStrategy random --polynomialMutationDistributionIndex 240.00523669189477 --linkedPolynomialMutationDistributionIndex 26.397943132686486 --uniformMutationPerturbation 0.7101453537910282 --nonUniformMutationPerturbation 0.3600168513410425 --crossover wholeArithmetic --crossoverProbability 0.7101198395275405 --crossoverRepairStrategy bounds --sbxDistributionIndex 384.6875425560933 --blxAlphaCrossoverAlphaValue 0.4254317723606224 --differentialEvolutionCrossover RAND_1_BIN --CR 0.4355614777286216 --F 0.985113013041491 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.45280114914465813 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 25000, "");

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
                + "--maximumNumberOfEvaluations 25000 "
                + "--neighborhoodSize 34 --maximumNumberOfReplacedSolutions 4 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 16.54697488039401 --pbiTheta 76.48625511954026 --algorithmResult externalArchive --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation linkedPolynomial --mutationProbabilityFactor 0.5569092364004489 --mutationRepairStrategy round --polynomialMutationDistributionIndex 93.57346407279815 --linkedPolynomialMutationDistributionIndex 18.266520132754916 --uniformMutationPerturbation 0.4589094466287788 --nonUniformMutationPerturbation 0.3772401189236081 --crossover wholeArithmetic --crossoverProbability 0.2818193035173884 --crossoverRepairStrategy round --sbxDistributionIndex 175.31215246222513 --blxAlphaCrossoverAlphaValue 0.7155001703429535 --differentialEvolutionCrossover RAND_1_BIN --CR 0.26670274608984584 --F 0.5050515137877507 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.08215703144414277 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 25000, "");

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
                + "--maximumNumberOfEvaluations 25000 "
                + "--neighborhoodSize 29 --maximumNumberOfReplacedSolutions 4 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 4.587991240469099 --pbiTheta 38.5853863940596 --algorithmResult externalArchive --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation polynomial --mutationProbabilityFactor 0.6229024302314088 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 19.520750767184406 --linkedPolynomialMutationDistributionIndex 381.72305017716434 --uniformMutationPerturbation 0.8872430438127202 --nonUniformMutationPerturbation 0.8722759993704883 --crossover wholeArithmetic --crossoverProbability 0.7192361014626819 --crossoverRepairStrategy round --sbxDistributionIndex 94.65950707713246 --blxAlphaCrossoverAlphaValue 0.7326887518595273 --differentialEvolutionCrossover RAND_1_BIN --CR 0.24573628337186537 --F 0.9859872455749441 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.1094499284198068 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD((DoubleProblem) experimentProblem.getProblem(), 100, 25000, "");

    autoMOEAD.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoMOEAD.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "MOEAD3000", experimentProblem, run));
  }
}
