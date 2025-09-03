package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.meta.MetaAsyncNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.evolver.util.problemfamilyinfo.ProblemFamilyInfo;
import org.uma.evolver.util.problemfamilyinfo.WFG2DProblemFamilyInfo;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link DoubleNSGAII} using the WFG
 * problems as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class AsyncNSGAIIOptimizingNSGAIIForBenchmarkWFG {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml" ;

    // Step 1: Select the target problem
    ProblemFamilyInfo<DoubleSolution> problemFamilyInfo = new WFG2DProblemFamilyInfo() ;

    List<Problem<DoubleSolution>> trainingSet = problemFamilyInfo.problemList() ;
    List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts() ;

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new InvertedGenerationalDistancePlus());
    var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
    var baseAlgorithm = new DoubleNSGAII(100, parameterSpace);
    var maximumNumberOfEvaluations = problemFamilyInfo.evaluationsToOptimize() ;
    int numberOfIndependentRuns = 1;

    EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations) ;

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
                baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            evaluationBudgetStrategy,
            numberOfIndependentRuns);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double builder
    int maxEvaluations = 2000;
    int numberOfCores = 8;

    AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii = new MetaAsyncNSGAIIBuilder(metaOptimizationProblem)
                    .setNumberOfCores(numberOfCores)
                    .setPopulationSize(50)
                    .setMaxEvaluations(2000)
                    .build() ;


    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "AsyncNSGA-II",
            metaOptimizationProblem,
            "WFG",
            indicators,
            "RESULTS/NSGAII/" + "WFG");

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(100, outputResults);

    var evaluationObserver = new EvaluationObserver(500);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "NSGA-II, " + "WFG",
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSet.get(0).name(),
            100);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    nsgaii.run();

    // Step 6: Write results
    outputResults.updateEvaluations(maxEvaluations);
    outputResults.writeResultsToFiles(nsgaii.result());

    System.exit(0);
  }
}
