package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithm;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.ParameterManagement;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIRunner {

  public static void main(String[] args) throws IOException {
    CrossoverOperator<DoubleSolution> crossover;
    MutationOperator<DoubleSolution> mutation;

    int populationSize = 100;
    int maxEvaluations = 1000;
    int numberOfCores = 10;

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT4();
    ConfigurableAlgorithm configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 5000);
    var configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithm,
        "resources/referenceFronts/ZDT4.csv",
        indicators, 1);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    long initTime = System.currentTimeMillis();

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<DoubleSolution>(
            numberOfCores, configurableProblem, populationSize, crossover, mutation,
            new TerminationByEvaluations(maxEvaluations));

    EvaluationObserver evaluationObserver = new EvaluationObserver(10);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II",
            80, 100, null, indicators.get(0).name(), indicators.get(1).name());

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(runTimeChartObserver);

    nsgaii.run();

    long endTime = System.currentTimeMillis();

    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>();
    nonDominatedSolutionsArchive.addAll(nsgaii.getResult());
    String problemDescription =
        "Async NSGA-II" + "." + problemWhoseConfigurationIsSearchedFor.name() + "."
            + indicators.get(0).name() + "." + indicators.get(1).name() + "."
            + problemWhoseConfigurationIsSearchedFor.numberOfObjectives();
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(
            new DefaultFileOutputContext("VAR." + problemDescription + ".csv", ","))
        .setFunFileOutputContext(
            new DefaultFileOutputContext("FUN." + problemDescription + ".csv", ","))
        .print();

    ParameterManagement.writeDecodedSolutionsFoFile(configurableProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(), "VAR." + problemDescription + ".Conf.csv");

    ParameterManagement.writeDecodedSolutionsDoubleValuesFoFile(
        configurableProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(),
        "VAR." + problemDescription + ".Conf.DoubleValues.csv");
    System.exit(0);
  }
}
