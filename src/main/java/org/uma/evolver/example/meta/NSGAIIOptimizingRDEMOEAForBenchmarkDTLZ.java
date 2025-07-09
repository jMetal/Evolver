package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.rdsmoea.RDEMOEADouble;
import org.uma.evolver.algorithm.meta.MetaNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.evolver.util.problemfamilyinfo.DTLZ3DProblemFamilyInfo;
import org.uma.evolver.util.problemfamilyinfo.ProblemFamilyInfo;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * This class implements a meta-optimization process using NSGA-II to automatically configure and
 * optimize the parameters of the RDE-MOEA (Ranking and Density Estimation Multi-Objective
 * Evolutionary Algorithm) for the DTLZ benchmark problem family.
 *
 * &lt;p&gt;The optimization process involves:
 * &lt;ol&gt;
 *   &lt;li&gt;Defining a set of DTLZ problems as the training set</li>
 *   &lt;li&gt;Configuring NSGA-II as the meta-optimizer to find optimal parameter settings for RDE-MOEA&lt;/li>
 *   &lt;li&gt;Using quality indicators (Epsilon and Normalized Hypervolume) to evaluate the performance</li>
 *   &lt;li&gt;Running multiple independent optimizations to ensure robust results</li>
 * </ol>
 *
 * &lt;p&gt;The class demonstrates how to:
 * &lt;ul&gt;
 *   &lt;li&gt;Set up a meta-optimization problem for algorithm configuration</li>
 *   &lt;li&gt;Use the DTLZ problem family for benchmarking</li>
 *   &lt;li&gt;Configure and run NSGA-II as a meta-optimizer</li>
 *   &lt;li&gt;Handle evaluation and visualization of results</li>
 * </ul>
 *
 * &lt;p&gt;Example usage:
 * <pre&gt;{@code
 * public static void main(String[] args) throws IOException {
 *   // The main method demonstrates how to set up and run the meta-optimization process
 *   // for configuring RDE-MOEA parameters on the DTLZ benchmark
 * }
 * }</pre>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 * @see RDEMOEADouble The RDE-MOEA implementation being optimized
 * @see DTLZ3DProblemFamilyInfo Information about the DTLZ problem family used for optimization
 * @see MetaOptimizationProblem The meta-optimization problem definition
 */
public class NSGAIIOptimizingRDEMOEAForBenchmarkDTLZ {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem
    ProblemFamilyInfo<DoubleSolution> problemFamilyInfo = new DTLZ3DProblemFamilyInfo();

    List<Problem<DoubleSolution>> trainingSet = problemFamilyInfo.problemList() ;
    List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts() ;

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    var configurableAlgorithm = new RDEMOEADouble(100);
    var maximumNumberOfEvaluations = problemFamilyInfo.evaluationsToOptimize() ;
    int numberOfIndependentRuns = 1;

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            configurableAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            maximumNumberOfEvaluations,
            numberOfIndependentRuns);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double builder
    int maxEvaluations = 2000;
    int numberOfCores = 8 ;

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = 
        new MetaNSGAIIBuilder(metaOptimizationProblem)
            .setMaxEvaluations(maxEvaluations)
            .setNumberOfCores(numberOfCores)
            .build();

    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "RDEMOEAD",
            metaOptimizationProblem,
            trainingSet.get(0).name(),
            indicators,
            "RESULTS/RDEMOEAD" + "DTLZ");

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(1, maxEvaluations, outputResults);

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "RDEMOEAD, " + trainingSet.get(0).name(),
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSet.get(0).name(),
            1);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    nsgaii.run();

    // Step 6: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResults.updateEvaluations(maxEvaluations);
    outputResults.writeResultsToFiles(nsgaii.result());

    System.exit(0);
  }
}
