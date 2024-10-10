package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.component.MultiThreadedMetaOptimizationProblemEvaluation;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAIIDE;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link ConfigurableNSGAII} using
 * problem {@link LZ09F2} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingNSGAIIForProblemZDT4 {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem
    var indicators = List.of(new Epsilon(), new InvertedGenerationalDistancePlus());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT4();
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableNSGAII})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 1000);
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
        .setEvaluation(new MultiThreadedMetaOptimizationProblemEvaluation(8, configurableProblem))
        .build();

    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor.name(), indicators,
        "RESULTS/NSGAII/" + problemWhoseConfigurationIsSearchedFor.name());

    var evaluationObserver = new EvaluationObserver(1);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "NSGA-II, " + problemWhoseConfigurationIsSearchedFor.name(), indicators.get(0).name(),
            indicators.get(1).name(), problemWhoseConfigurationIsSearchedFor.name(), 1);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(1,
        maxEvaluations, outputResultsManagement);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
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
