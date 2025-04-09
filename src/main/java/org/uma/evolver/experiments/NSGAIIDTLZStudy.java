package org.uma.evolver.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;

public class NSGAIIDTLZStudy {

  private static final int INDEPENDENT_RUNS = 30;
  private static final int POPULATION_SIZE = 100;
  private static int MAX_EVALUATIONS = 40000;

  public static void main(String[] args) throws IOException {
    Check.that(args.length == 1, "Missing argument: experimentBaseDirectory");

    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    problemList.add(new ExperimentProblem<>(new DTLZ1()).setReferenceFront("DTLZ1.3D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ2()).setReferenceFront("DTLZ2.3D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ3()).setReferenceFront("DTLZ3.3D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ4()).setReferenceFront("DTLZ4.3D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ5()).setReferenceFront("DTLZ5.3D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ6()).setReferenceFront("DTLZ6.3D.csv"));
    problemList.add(new ExperimentProblem<>(new DTLZ7()).setReferenceFront("DTLZ7.3D.csv"));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("EvolverNSGAIIDTLZ.03")
            .setAlgorithmList(algorithmList)
            .setProblemList(problemList)
            .setReferenceFrontDirectory("resources/referenceFrontsCSV")
            .setExperimentBaseDirectory(experimentBaseDirectory)
            .setOutputParetoFrontFileName("FUN")
            .setOutputParetoSetFileName("VAR")
            .setIndicatorList(
                Arrays.asList(
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
    // new GenerateWilcoxonTestTablesWithR<>(experiment).run();
    // new GenerateFriedmanHolmTestTables<>(experiment).run();
    // new GenerateFriedmanTestTables<>(experiment).run();
    // new GenerateBoxplotsWithR<>(experiment).setRows(3).setColumns(3).setDisplayNotch().run();
    // new GenerateHtmlPages<>(experiment, StudyVisualizer.TYPE_OF_FRONT_TO_SHOW.MEDIAN).run();
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
        nsgaii(algorithms, run, experimentProblem);
        nsgaii1k(algorithms, run, experimentProblem);
        nsgaii2k(algorithms, run, experimentProblem);
        nsgaii3k(algorithms, run, experimentProblem);
      }
    }
    return algorithms;
  }

  private static void nsgaii(
      List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
      int run,
      ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--algorithmResult population " +
                " --createInitialSolutions random " +
                "--offspringPopulationSize 100 " +
                "--variation crossoverAndMutationVariation " +
                "--crossover SBX " +
                "--crossoverProbability 0.9 " +
                "--crossoverRepairStrategy bounds " +
                "--sbxDistributionIndex 20.0 " +
                "--mutation polynomial " +
                "--mutationProbabilityFactor 1.0 " +
                "--mutationRepairStrategy bounds " +
                "--polynomialMutationDistributionIndex 20.0 " +
                "--selection tournament " +
                "--selectionTournamentSize 2 \n")
            .split("\\s+");

    ConfigurableNSGAII algorithm =
        new ConfigurableNSGAII(
            (DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm.build(), "NSGAII", experimentProblem, run));
  }
  private static void nsgaii1k(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
          int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 22 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 50 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9423119061124078 --crossoverRepairStrategy bounds --sbxDistributionIndex 171.6338531747379 --blxAlphaCrossoverAlphaValue 0.8093172092328885 --mutation uniform --mutationProbabilityFactor 0.8600581582415145 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 309.1951290603569 --linkedPolynomialMutationDistributionIndex 41.322406405771225 --uniformMutationPerturbation 0.27069228836463316 --nonUniformMutationPerturbation 0.40675375210985387 --selection random --selectionTournamentSize 5 \n ")
            .split("\\s+");

    ConfigurableNSGAII algorithm =
            new ConfigurableNSGAII(
                    (DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm.build(), "NSGAII1K", experimentProblem, run));
  }

  private static void nsgaii2k(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
          int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 22 --externalArchive crowdingDistanceArchive --createInitialSolutions random --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9423552134053876 --crossoverRepairStrategy round --sbxDistributionIndex 127.85806191505195 --blxAlphaCrossoverAlphaValue 0.9225664433923035 --mutation uniform --mutationProbabilityFactor 0.8564702727337172 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 312.06655848388755 --linkedPolynomialMutationDistributionIndex 103.02840786538992 --uniformMutationPerturbation 0.23074065776355066 --nonUniformMutationPerturbation 0.9112203824487257 --selection random --selectionTournamentSize 5 \n")
            .split("\\s+");

    ConfigurableNSGAII algorithm =
            new ConfigurableNSGAII(
                    (DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm.build(), "NSGAII2K", experimentProblem, run));
  }

  private static void nsgaii3k(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms,
          int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {

    String[] parameters =
        ("--algorithmResult externalArchive --populationSizeWithArchive 34 --externalArchive unboundedArchive --createInitialSolutions random --offspringPopulationSize 20 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9563297991540735 --crossoverRepairStrategy bounds --sbxDistributionIndex 67.31429937791252 --blxAlphaCrossoverAlphaValue 0.42549762698821153 --mutation uniform --mutationProbabilityFactor 0.7877484170557963 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 72.17459548865683 --linkedPolynomialMutationDistributionIndex 147.26262651735544 --uniformMutationPerturbation 0.4178725969506182 --nonUniformMutationPerturbation 0.8377391289216694 --selection random --selectionTournamentSize 5 \n")
            .split("\\s+");

    ConfigurableNSGAII algorithm =
            new ConfigurableNSGAII(
                    (DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm.build(), "NSGAII3K", experimentProblem, run));
  }
}
