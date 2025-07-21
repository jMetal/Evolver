package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.mopso.MOPSO;
import org.uma.evolver.algorithm.meta.MetaNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.OutputResults;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.zcat.DefaultZCATSettings;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running NSGA-II as meta-optimizer to configure {@link MOPSO} using
 * problem {@link ZDT4} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingMOPSOForProblemZCAT3 {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "resources/parameterSpaces/MOPSO.yaml" ;

    // Step 1: Select the target problem
    DefaultZCATSettings.numberOfObjectives = 3 ;
    List<Problem<DoubleSolution>> trainingSet = List.of(new ZCAT3());
    List<String> referenceFrontFileNames = List.of("resources/referenceFronts/ZCAT3.3D.csv");

    // Step 2: Set the parameters for the algorithm to be configured
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    var parameterSpace =
        new YAMLParameterSpace(yamlParameterSpaceFile, new MOPSOParameterFactory());
    System.out.println(parameterSpace);
    // var configurableAlgorithm = new MOEADDouble(100);
    var baseAlgorithm = new MOPSO(100, parameterSpace);

    var maximumNumberOfEvaluations = List.of(20000);
    int numberOfIndependentRuns = 1;

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            maximumNumberOfEvaluations,
            numberOfIndependentRuns);

    // Step 3: Set up and configure the meta-optimizer (NSGA-II) using the specialized double builder
    int maxEvaluations = 2000;
    int numberOfCores = 1;

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = 
        new MetaNSGAIIBuilder(metaOptimizationProblem, parameterSpace)
            .setMaxEvaluations(maxEvaluations)
            .setNumberOfCores(numberOfCores)
            .build();

    // Step 4: Create observers for the meta-optimizer
    var outputResults =
        new OutputResults(
            "MOPSO",
            metaOptimizationProblem,
            trainingSet.get(0).name(),
            indicators,
            "RESULTS/MOPSO/" + trainingSet.get(0).name());

    var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(1, maxEvaluations, outputResults);

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "MOPSO, " + trainingSet.get(0).name(),
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
