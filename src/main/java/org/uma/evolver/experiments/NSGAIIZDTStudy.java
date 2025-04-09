package org.uma.evolver.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.GenerateFriedmanHolmTestTables;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateWilcoxonTestTablesWithR;
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

public class NSGAIIZDTStudy {

  private static final int INDEPENDENT_RUNS = 30;
  private static final int POPULATION_SIZE = 100;
  private static int MAX_EVALUATIONS = 25000;

  public static void main(String[] args) throws IOException {
    Check.that(args.length == 1, "Missing argument: experimentBaseDirectory");

    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();

    problemList.add(new ExperimentProblem<>(new ZDT1()));
    problemList.add(new ExperimentProblem<>(new ZDT2()));
    problemList.add(new ExperimentProblem<>(new ZDT3()));
    problemList.add(new ExperimentProblem<>(new ZDT4()));
    problemList.add(new ExperimentProblem<>(new ZDT6()));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
        configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("EvolverNSGAIIZDT.03")
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

    //new ExecuteAlgorithms<>(experiment).run();

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
        ("--algorithmResult externalArchive --populationSizeWithArchive 29 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 5 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.665472957052981 --crossoverRepairStrategy round --sbxDistributionIndex 374.9849017752163 --blxAlphaCrossoverAlphaValue 0.12895780487254205 --mutation linkedPolynomial --mutationProbabilityFactor 1.7928008813882803 --mutationRepairStrategy random --polynomialMutationDistributionIndex 175.37431904444531 --linkedPolynomialMutationDistributionIndex 23.351315440095718 --uniformMutationPerturbation 0.9762899306316961 --nonUniformMutationPerturbation 0.6837882698506264 --selection tournament --selectionTournamentSize 4 \n ")
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
            ("--algorithmResult externalArchive --populationSizeWithArchive 26 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 10 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.7842089447334547 --crossoverRepairStrategy random --sbxDistributionIndex 65.67651793326992 --blxAlphaCrossoverAlphaValue 0.5486918307100471 --mutation polynomial --mutationProbabilityFactor 0.9625044769087703 --mutationRepairStrategy round --polynomialMutationDistributionIndex 19.102669898566432 --linkedPolynomialMutationDistributionIndex 83.13206456837084 --uniformMutationPerturbation 0.47656931844291106 --nonUniformMutationPerturbation 0.7302220718965483 --selection tournament --selectionTournamentSize 6 ")
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
            ("--algorithmResult externalArchive --populationSizeWithArchive 26 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9043533390938738 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.058802584370255 --blxAlphaCrossoverAlphaValue 0.12856563100163604 --mutation linkedPolynomial --mutationProbabilityFactor 0.7420943392862798 --mutationRepairStrategy random --polynomialMutationDistributionIndex 188.39287866759918 --linkedPolynomialMutationDistributionIndex 18.82925065749857 --uniformMutationPerturbation 0.9832071464945279 --nonUniformMutationPerturbation 0.665532195929286 --selection tournament --selectionTournamentSize 4")
                    .split("\\s+");

    ConfigurableNSGAII algorithm =
            new ConfigurableNSGAII(
                    (DoubleProblem) experimentProblem.getProblem(), POPULATION_SIZE, MAX_EVALUATIONS);
    algorithm.parse(parameters);

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm.build(), "NSGAII3K", experimentProblem, run));
  }
}
