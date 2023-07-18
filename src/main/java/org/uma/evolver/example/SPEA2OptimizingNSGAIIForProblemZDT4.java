package org.uma.evolver.example;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.spea2.SPEA2Builder;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.evaluator.impl.MultiThreadedSolutionListEvaluator;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running SPEA2 as meta-optimizer to configure {@link ConfigurableNSGAII} using
 * problem {@link ZDT4} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class SPEA2OptimizingNSGAIIForProblemZDT4 {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem (ZDT4)
    var indicators = List.of(new Epsilon(), new Spread());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT4();
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableNSGAII})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 15000);
    var configurableProblem = new MetaOptimizationProblem(configurableAlgorithm,
        referenceFrontFileName,
        indicators, 1);

    // Step 3: Set the parameters for the meta-optimizer (NSGAII)
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    var selection = new BinaryTournamentSelection<DoubleSolution>();

    int maxEvaluations = 2000;
    int populationSize = 50 ;
    var evaluator = new MultiThreadedSolutionListEvaluator<DoubleSolution>(8);
    var spea2 =
        new SPEA2Builder<>(configurableProblem, crossover, mutation)
            .setSelectionOperator(selection)
            .setPopulationSize(populationSize)
            .setMaxIterations(maxEvaluations/populationSize)
            .setSolutionListEvaluator(evaluator)
            .build();

    // Step 4: Run the meta-optimizer
    AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(spea2).execute();

    // Step 5: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + algorithmRunner.getComputingTime());

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor.name(), indicators,
        "RESULTS/NSGA-II/ZDT4");
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(spea2.result());

    System.exit(0);
  }
}
