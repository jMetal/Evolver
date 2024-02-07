package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link ConfigurableMOPSO} using problem
 * {@link ZDT4} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingMOPSOForProblemZDT4 {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the problem to be used as the training set
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT4();
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.ReferencePoints.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableMOPSO})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableMOPSO(
        problemWhoseConfigurationIsSearchedFor, 100, 15000);

    List<QualityIndicator> indicators = List.of(new HypervolumeMinus());
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

    int populationSize = 50;
    int offspringPopulationSize = 50;

    int maxEvaluations = 2000;
    Termination termination = new TerminationByEvaluations(maxEvaluations);

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = new NSGAIIBuilder<>(
        configurableProblem,
        populationSize,
        offspringPopulationSize,
        crossover,
        mutation)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, configurableProblem))
        .build();

    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, "ZDT4", indicators,
        "RESULTS/MOPSO/ZDT4RF");

    var evaluationObserver = new EvaluationObserver(50);

    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(100,
        maxEvaluations, outputResultsManagement);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    nsgaii.run();

    // Step 6: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.result());

    System.exit(0);
  }
}
