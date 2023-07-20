package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.pesa2.PESA2Builder;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.evaluator.impl.MultiThreadedSolutionListEvaluator;

/**
 * Class for running PESA2 as meta-optimizer to configure {@link ConfigurableMOPSO} using
 * problem {@link DTLZ5} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class PESA2OptimizingMOPSOForProblemDTLZ5 {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem (DTLZ5)
    var indicators = List.of(new Epsilon(), new Spread());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new DTLZ5();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ5.5D.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableNSGAII})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableMOPSO(
        problemWhoseConfigurationIsSearchedFor, 100, 10000);
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

    int maxEvaluations = 2000;
    var evaluator = new MultiThreadedSolutionListEvaluator<DoubleSolution>(8);

    var pesa2 =
        new PESA2Builder<>(configurableProblem, crossover, mutation)
            .setMaxEvaluations(maxEvaluations)
            .setPopulationSize(10)
            .setArchiveSize(1000)
            .setBisections(5)
            .setSolutionListEvaluator(evaluator)
            .build();

    // Step 4: Run the meta-optimizer
    AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(pesa2).execute();

    // Step 5: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + algorithmRunner.getComputingTime());

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor.name(), indicators,
        "RESULTS/NSGA-II/ZDT4");
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(pesa2.result());

    System.exit(0);
  }
}
