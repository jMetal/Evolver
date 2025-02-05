package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.MultiFocusMetaOptimizationProblem;
import org.uma.evolver.problemfamilyinfo.DTLZ3DProblemFamilyInfo;
import org.uma.evolver.problemfamilyinfo.ProblemFamilyInfo;
import org.uma.evolver.problemfamilyinfo.ZCATReducedProblemFamilyInfo;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.doubleproblem.impl.FakeDoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Class for running {@link AsynchronousMultiThreadedNSGAII} as meta-optimizer to configure
 * {@link ConfigurableNSGAII} using problem {@link DTLZ1} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingNSGAIIForProblemsZCAT {

  public static void main(String[] args) throws IOException {

    int numberOfCores;
    int runId;
    String outputDirectory ;

    if (args.length != 3) {
      throw new JMetalException("Arguments required: runId, number of cores, output directory");
    } else {
      runId = Integer.valueOf(args[0]);
      numberOfCores = Integer.valueOf((args[1]));
      outputDirectory = args[2] ;
    }

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    ProblemFamilyInfo problemFamilyInfo = new ZCATReducedProblemFamilyInfo();

    List<DoubleProblem> trainingSet = problemFamilyInfo.problemList();
    List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts();
    double trainingEvaluationsPercentage = 0.4;
    List<Integer> maxEvaluationsPerProblem =
            problemFamilyInfo.evaluationsToOptimize().stream()
                    .map(evaluations -> (int) (evaluations * trainingEvaluationsPercentage))
                    .toList();

    // Step 2: Set the parameters for the algorithm to be configured)
    ConfigurableAlgorithmBuilder configurableAlgorithm =
            new ConfigurableNSGAII(100);
    var configurableProblem = new MultiFocusMetaOptimizationProblem(configurableAlgorithm,
            trainingSet, referenceFrontFileNames,
            indicators, maxEvaluationsPerProblem, 30);

    // Step 3: Set the parameters for the meta-optimizer (NSGAII)
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 50;
    int maxEvaluations = 3000;

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
        new AsynchronousMultiThreadedNSGAII<>(
            numberOfCores, configurableProblem, populationSize, crossover, mutation,
            new TerminationByEvaluations(maxEvaluations));

    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters =
        new OutputResultsManagementParameters(
            "AsyncNSGA-II",
            configurableProblem,
            problemFamilyInfo.name(),
            indicators,
                outputDirectory + "/AsyncNSGAIINSGAII/"+problemFamilyInfo.name()+ ".MEAN." +runId);

    var evaluationObserver = new EvaluationObserver(100);

    /*
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "AsyncNSGA-II",
            80, 100, null, indicators.get(0).name(), indicators.get(1).name());
*/
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(100,
        maxEvaluations, outputResultsManagement);

    nsgaii.getObservable().register(evaluationObserver);
    //nsgaii.getObservable().register(runTimeChartObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    long initTime = System.currentTimeMillis();
    nsgaii.run();
    long endTime = System.currentTimeMillis();

    // Step 6: Write results
    System.out.println("Total computing time: " + (endTime - initTime)) ;

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());

    System.exit(0);
  }
}
