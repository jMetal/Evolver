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

public class ZDTNSGAIIGECCO2025Study {
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
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ZDTNSGAIIGECCO2025Study")
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
            + "--maximumNumberOfEvaluations 25000 "
            + "--populationSize 100 "
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

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);

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
                + "--maximumNumberOfEvaluations 25000 "
                + "--populationSize 100 "
                + "--algorithmResult population --populationSizeWithArchive 47 --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.6903059742715068 --crossoverRepairStrategy random --sbxDistributionIndex 321.2616304308958 --blxAlphaCrossoverAlphaValue 0.9304227135752793 --mutation linkedPolynomial --mutationProbabilityFactor 1.8027115289126232 --mutationRepairStrategy random --polynomialMutationDistributionIndex 304.61616796562 --linkedPolynomialMutationDistributionIndex 9.649569037516262 --uniformMutationPerturbation 0.16840207289539277 --nonUniformMutationPerturbation 0.5447738929695891 --selection tournament --selectionTournamentSize 6 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
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
                + "--maximumNumberOfEvaluations 25000 "
                + "--populationSize 100 "
                + "--algorithmResult externalArchive --populationSizeWithArchive 18 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.8758253509017933 --crossoverRepairStrategy round --sbxDistributionIndex 229.7327711405189 --blxAlphaCrossoverAlphaValue 0.9295985898060788 --mutation polynomial --mutationProbabilityFactor 1.2873918152709898 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.291146731672633 --linkedPolynomialMutationDistributionIndex 128.5090501336901 --uniformMutationPerturbation 0.6692190500401405 --nonUniformMutationPerturbation 0.7541845052387055 --selection tournament --selectionTournamentSize 4 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
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
                + "--maximumNumberOfEvaluations 25000 "
                + "--populationSize 100 "
                + "--algorithmResult externalArchive --populationSizeWithArchive 41 --externalArchive crowdingDistanceArchive --createInitialSolutions scatterSearch --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.7586849790311361 --crossoverRepairStrategy bounds --sbxDistributionIndex 26.83304918608185 --blxAlphaCrossoverAlphaValue 0.009874932564419403 --mutation linkedPolynomial --mutationProbabilityFactor 0.9300323828641616 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 288.46861724146777 --linkedPolynomialMutationDistributionIndex 21.513752404782675 --uniformMutationPerturbation 0.736209358225424 --nonUniformMutationPerturbation 0.4201608526237486 --selection tournament --selectionTournamentSize 7 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII2K", experimentProblem, run));
  }

  private static void nsgaii3000(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);\n--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--maximumNumberOfEvaluations 25000 "
                + "--populationSize 100 "
                + "--algorithmResult externalArchive --populationSizeWithArchive 10 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 5 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.8984153416103235 --crossoverRepairStrategy round --sbxDistributionIndex 5.639146718982451 --blxAlphaCrossoverAlphaValue 0.8719212077675329 --mutation linkedPolynomial --mutationProbabilityFactor 0.6765174224254235 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 186.2783811412129 --linkedPolynomialMutationDistributionIndex 18.837327868867042 --uniformMutationPerturbation 0.9282112628295636 --nonUniformMutationPerturbation 0.5812451775569258 --selection random --selectionTournamentSize 3 \n")
            .split("\\s+");

    var autoNSGAII = new ConfigurableNSGAII((DoubleProblem) experimentProblem.getProblem(), 100, 25000);
    autoNSGAII.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = autoNSGAII.build() ;

    algorithms.add(new ExperimentAlgorithm<>(nsgaII, "NSGAII3k", experimentProblem, run));
  }
}
