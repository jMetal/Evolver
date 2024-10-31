package org.uma.evolver.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
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
import org.uma.jmetal.problem.multiobjective.dtlz.*;
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

public class MOEADDTLZ3D {

  private static final int INDEPENDENT_RUNS = 25;
  private static final String weightVectorDirectory = "resources/weightVectors" ;

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
        new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("MOEADDTLZ3D")
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
        moeadEvol(algorithms, run, experimentProblem);
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
            + "--normalizeObjectives False "
            + "--epsilonParameterForNormalizing 4 "
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

    int populationSize = 91;
    int maximumNumberOfEvaluations = 40000;
    ConfigurableMOEAD moead = new ConfigurableMOEAD(
        (DoubleProblem) experimentProblem.getProblem(), populationSize, maximumNumberOfEvaluations, weightVectorDirectory);
    moead.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();

    algorithms.add(
        new ExperimentAlgorithm<>(algorithm, "MOEAD", experimentProblem, run));
  }

  private static void moeadEvol(
          List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms, int run,
          ExperimentProblem<DoubleSolution> experimentProblem) {
    String[] parameters =
        ("--referenceFrontFileName "
                + experimentProblem.getReferenceFront()
                + " "
                + "--neighborhoodSize 15 --maximumNumberOfReplacedSolutions 4 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 17.762517798148405 --pbiTheta 36.493745043844214 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.7490456303400791 --mutationRepairStrategy round --polynomialMutationDistributionIndex 278.39055193143423 --linkedPolynomialMutationDistributionIndex 386.7145843012343 --uniformMutationPerturbation 0.4317668362573911 --nonUniformMutationPerturbation 0.6804841027955307 --crossover SBX --crossoverProbability 0.1688200974619386 --crossoverRepairStrategy random --sbxDistributionIndex 71.06092756667113 --blxAlphaCrossoverAlphaValue 0.3515427165866335 --differentialEvolutionCrossover RAND_1_EXP --CR 0.7635848027110335 --F 0.49588434265982173 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.4725919661222408\n ")
            .split("\\s+");

    int populationSize = 91;
    int maximumNumberOfEvaluations = 40000;
    ConfigurableMOEAD moead = new ConfigurableMOEAD(
            (DoubleProblem) experimentProblem.getProblem(), populationSize, maximumNumberOfEvaluations, weightVectorDirectory);
    moead.parse(parameters);

    EvolutionaryAlgorithm<DoubleSolution> algorithm = moead.build();

    algorithms.add(
            new ExperimentAlgorithm<>(algorithm, "MOEADConf", experimentProblem, run));
  }
}
