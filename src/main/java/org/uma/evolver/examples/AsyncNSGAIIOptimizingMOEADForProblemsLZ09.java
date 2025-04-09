package org.uma.evolver.examples;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.MultiFocusMetaOptimizationProblem;
import org.uma.evolver.problemfamilyinfo.DTLZ3DProblemFamilyInfo;
import org.uma.evolver.problemfamilyinfo.LZ09ProblemFamilyInfo;
import org.uma.evolver.problemfamilyinfo.ProblemFamilyInfo;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class for running {@link AsynchronousMultiThreadedNSGAII} as meta-optimizer to configure
 * {@link ConfigurableNSGAII} using problem {@link DTLZ1} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingMOEADForProblemsLZ09 {

  public static void main(String[] args) throws IOException {
    String weightVectorFilesDirectory = "resources/weightVectors";

    int numberOfCores;
    int runId;
    String outputDirectory ;
    double trainingEvaluationsPercentage ;

    if (args.length != 4) {
      throw new JMetalException("Arguments required: runId, number of cores, output directory trainingEvaluationsPercentage");
    } else {
      runId = Integer.valueOf(args[0]);
      numberOfCores = Integer.valueOf((args[1]));
      outputDirectory = args[2] ;
      trainingEvaluationsPercentage = Double.valueOf(args[3]);
    }

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    ProblemFamilyInfo problemFamilyInfo = new LZ09ProblemFamilyInfo();

    List<DoubleProblem> trainingSet = problemFamilyInfo.problemList();
    List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts();
    List<Integer> maxEvaluationsPerProblem =
            problemFamilyInfo.evaluationsToOptimize().stream()
                    .map(evaluations -> (int) (evaluations * trainingEvaluationsPercentage))
                    .toList();

    // Step 2: Set the parameters for the algorithm to be configured
    ConfigurableAlgorithmBuilder configurableAlgorithm =
            new ConfigurableMOEAD(100, weightVectorFilesDirectory);
    var configurableProblem =
            new MultiFocusMetaOptimizationProblem(
                    configurableAlgorithm,
                    trainingSet,
                    referenceFrontFileNames,
                    indicators,
                    maxEvaluationsPerProblem,
                    1);

    // Step 3: Set the parameters for the meta-optimizer (NSGAII)
    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 50;
    int maxEvaluations = 2000;

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
            new AsynchronousMultiThreadedNSGAII<>(
                    numberOfCores,
                    configurableProblem,
                    populationSize,
                    crossover,
                    mutation,
                    new TerminationByEvaluations(maxEvaluations));

    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters =
            new OutputResultsManagementParameters(
                    "AsyncNSGAII",
                    configurableProblem,
                    problemFamilyInfo.name(),
                    indicators,
                    outputDirectory + "/AsyncNSGAIIMOEAD/"+problemFamilyInfo.name()+ ".MEAN." +runId);

    var evaluationObserver = new EvaluationObserver(100);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);
    var writeExecutionDataToFilesObserver =
            new WriteExecutionDataToFilesObserver(100, maxEvaluations, outputResultsManagement);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
            new RunTimeChartObserver<>(
                    "Base optimizer: MOEA/D", 80, 100, null, indicators.get(0).name(), indicators.get(1).name());


    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(writeExecutionDataToFilesObserver);
    nsgaii.getObservable().register(runTimeChartObserver);


    // Step 5: Run the meta-optimizer
    long initTime = System.currentTimeMillis();
    nsgaii.run();
    long endTime = System.currentTimeMillis();

    // Step 6: Write results
    System.out.println("Total computing time: " + (endTime - initTime));

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(nsgaii.getResult());

    System.exit(0);
  }
}
