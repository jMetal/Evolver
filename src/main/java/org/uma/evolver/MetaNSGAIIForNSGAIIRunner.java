package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
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
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1_2D;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MetaNSGAIIForNSGAIIRunner {

  public static void main(String[] args) throws IOException {

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new DTLZ1_2D();
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 50, 20000);
    var configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithm,
        "resources/referenceFronts/DTLZ1.2D.csv",
        indicators, 1);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 50;
    int offspringPopulationSize = 50;

    int maxEvaluations = 3000 ;
    Termination termination = new TerminationByEvaluations(maxEvaluations);

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor, indicators,
        "outputFiles");
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = new NSGAIIBuilder<>(
        configurableProblem,
        populationSize,
        offspringPopulationSize,
        crossover,
        mutation)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, configurableProblem))
        .build();

    var evaluationObserver = new EvaluationObserver(10);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>("NSGA-II", indicators.get(0).name(), indicators.get(1).name(), problemWhoseConfigurationIsSearchedFor.name(), 50);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(
        List.of(2000, 3000), outputResultsManagement);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    nsgaii.run();

    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.result());

    //System.exit(0) ;
  }
}
