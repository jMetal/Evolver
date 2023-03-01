package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithm;
import org.uma.evolver.algorithm.impl.ConfigurableMOPSO;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.ParameterManagement;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MetaNSGAIIForMOPSORunner {

  public static void main(String[] args) throws IOException {
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT1();
    ConfigurableAlgorithm configurableAlgorithm = new ConfigurableMOPSO(
        problemWhoseConfigurationIsSearchedFor, 100, 5000);
    var configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithm,
        "resources/ZDT1.csv",
        indicators,1);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 50;
    int offspringPopulationSize = 50;

    Termination termination = new TerminationByEvaluations(2000);

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = new NSGAIIBuilder<>(
        configurableProblem,
        populationSize,
        offspringPopulationSize,
        crossover,
        mutation)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, configurableProblem))
        .build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(10);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOPSO - " + problemWhoseConfigurationIsSearchedFor.name(), 80, 100, null,
            indicators.get(0).name(),
            indicators.get(1).name());

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(runTimeChartObserver);

    nsgaii.run();

    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    var outputResultsManagement = new OutputResultsManagement("results");
    outputResultsManagement.writeResultsToFiles(nsgaii.result(), "MOPSO", configurableProblem,
        problemWhoseConfigurationIsSearchedFor, indicators);

    //System.exit(0) ;
  }
}
