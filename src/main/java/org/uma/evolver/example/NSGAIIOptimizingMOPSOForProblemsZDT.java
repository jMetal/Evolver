package org.uma.evolver.example;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.problem.MultiFocusMetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
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
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link ConfigurableMOPSO} using problems
 * {@link ZDT1}, {@link ZDT2}, {@link ZDT3}, {@link ZDT4}, and {@link ZDT6} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingMOPSOForProblemsZDT {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the training set problems  (ZDT1, ZDT2, ZDT3, ZDT4, ZDT6)
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    List<DoubleProblem> trainingSet = List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(),
        new ZDT6());
    List<String> referenceFrontFileNames = List.of(
        "resources/referenceFronts/ZDT1.csv",
        "resources/referenceFronts/ZDT2.csv",
        "resources/referenceFronts/ZDT3.csv",
        "resources/referenceFronts/ZDT4.csv",
        "resources/referenceFronts/ZDT6.csv");
    List<Integer> maxEvaluationsPerProblem = List.of(6000, 6000, 6000, 10000, 6000);

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableMOPSO})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableMOPSO(
        new FakeDoubleProblem(), 100, 15000);
    var configurableProblem = new MultiFocusMetaOptimizationProblem(configurableAlgorithm,
        trainingSet, referenceFrontFileNames,
        indicators, maxEvaluationsPerProblem);

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
        "NSGA-II", configurableProblem, "ZDT", indicators,
        "RESULTS/MOPSO/ZDT");

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "NSGA-II, " + "ZDT", indicators.get(0).name(),
            indicators.get(1).name(), "ZDT", 50);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(25,
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
