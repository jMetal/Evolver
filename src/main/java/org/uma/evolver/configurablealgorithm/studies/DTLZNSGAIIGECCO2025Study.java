package org.uma.evolver.configurablealgorithm.studies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.uma.jmetal.problem.multiobjective.dtlz.*;
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

public class DTLZNSGAIIGECCO2025Study {
  private static final int INDEPENDENT_RUNS = 30;

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
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("DTLZNSGAIIGECCO2025Study")
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
        nsgaii(algorithms, run, experimentProblem);
        nsgaii500(algorithms, run, experimentProblem);
        nsgaii1000(algorithms, run, experimentProblem);
        nsgaii2000(algorithms, run, experimentProblem);
        nsgaii3000(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }


  private static void nsgaii(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName " + experimentProblem.getReferenceFront() + " "
            + "--algorithmResult population  "
            + "--createInitialSolutions random "
            + "--offspringPopulationSize 100 "
            + "--variation crossoverAndMutationVariation --crossover SBX "
            + "--crossoverProbability 0.9 "
            + "--crossoverRepairStrategy bounds "
            + "--sbxDistributionIndex 20.0 "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--polynomialMutationDistributionIndex 20.0 "
            + "--mutationRepairStrategy bounds "
            + "--selection tournament "
            + "--selectionTournamentSize 2 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 40000);

    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII", experimentProblem, run));
  }

  private static void nsgaii500(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--algorithmResult population --populationSizeWithArchive 23 --externalArchive unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.7372334157763367 --crossoverRepairStrategy round --sbxDistributionIndex 29.435835993520236 --blxAlphaCrossoverAlphaValue 0.5163485550913485 --mutation uniform --mutationProbabilityFactor 0.4430330407622991 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 393.9422729008177 --linkedPolynomialMutationDistributionIndex 237.82510558985481 --uniformMutationPerturbation 0.44083798367649607 --nonUniformMutationPerturbation 0.5060009656163938 --selection random --selectionTournamentSize 9 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII500", experimentProblem, run));
  }

  private static void nsgaii1000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--algorithmResult externalArchive --populationSizeWithArchive 62 --externalArchive crowdingDistanceArchive --createInitialSolutions random --offspringPopulationSize 5 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.7458068971293907 --crossoverRepairStrategy random --sbxDistributionIndex 81.5309423090636 --blxAlphaCrossoverAlphaValue 0.8166687078185118 --mutation uniform --mutationProbabilityFactor 0.7309648047757319 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 303.67619429400935 --linkedPolynomialMutationDistributionIndex 156.95829967539538 --uniformMutationPerturbation 0.21467352428566588 --nonUniformMutationPerturbation 0.16988771035934178 --selection random --selectionTournamentSize 8 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII1K", experimentProblem, run));
  }

  private static void nsgaii2000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--algorithmResult population --populationSizeWithArchive 51 --externalArchive unboundedArchive --createInitialSolutions random --offspringPopulationSize 100 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9976383960993702 --crossoverRepairStrategy random --sbxDistributionIndex 108.01927398139804 --blxAlphaCrossoverAlphaValue 0.9770186845181825 --mutation uniform --mutationProbabilityFactor 0.5350166979138014 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 322.1488963037457 --linkedPolynomialMutationDistributionIndex 368.25357619122195 --uniformMutationPerturbation 0.2109800002629293 --nonUniformMutationPerturbation 0.13197742053427397 --selection tournament --selectionTournamentSize 7 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII2K", experimentProblem, run));
  }

  private static void nsgaii3000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
            ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--algorithmResult externalArchive --populationSizeWithArchive 128 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9939735855009 --crossoverRepairStrategy round --sbxDistributionIndex 97.7892500375028 --blxAlphaCrossoverAlphaValue 0.6605947190017734 --mutation uniform --mutationProbabilityFactor 0.7999577405382269 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 86.83766511517442 --linkedPolynomialMutationDistributionIndex 282.67714995073896 --uniformMutationPerturbation 0.2784653434636453 --nonUniformMutationPerturbation 0.8670278735443806 --selection tournament --selectionTournamentSize 7 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 40000);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII3k", experimentProblem, run));
  }
}
