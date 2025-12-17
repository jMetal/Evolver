package org.uma.evolver.example.meta;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.base.mopso.BaseMOPSO;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.algorithm.meta.MetaNSGAIIBuilder;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.EvaluationBudgetStrategy;
import org.uma.evolver.metaoptimizationproblem.evaluationbudgetstrategy.FixedEvaluationsStrategy;
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
 * Class for running NSGA-II as meta-optimizer to configure {@link BaseMOPSO} using
 * problem {@link ZDT4} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIOptimizingMOPSOForProblemZCAT3 {

  // Meta-optimizer configuration
  private static final int META_MAX_EVALUATIONS = 2000;
  private static final int NUMBER_OF_CORES = 1;

  // Base-level algorithm configuration
  private static final int BASE_POPULATION_SIZE = 100;
  private static final int NUMBER_OF_INDEPENDENT_RUNS = 1;
  private static final int BASE_MAX_EVALUATIONS = 20000;

  // Observer configuration
  private static final int EVALUATION_OBSERVER_FREQUENCY = 50;
  private static final int WRITE_FREQUENCY = 1;
  private static final int PLOT_UPDATE_FREQUENCY = 1;

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "MOPSO.yaml" ;

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
    var baseAlgorithm = new BaseMOPSO(BASE_POPULATION_SIZE, parameterSpace);

    var maximumNumberOfEvaluations = List.of(BASE_MAX_EVALUATIONS);
    int numberOfIndependentRuns = NUMBER_OF_INDEPENDENT_RUNS;

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
    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
        new MetaNSGAIIBuilder(metaOptimizationProblem, new NSGAIIDoubleParameterSpace())
            .setMaxEvaluations(META_MAX_EVALUATIONS)
            .setNumberOfCores(NUMBER_OF_CORES)
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
        new WriteExecutionDataToFilesObserver(WRITE_FREQUENCY, outputResults);

    var evaluationObserver = new EvaluationObserver(EVALUATION_OBSERVER_FREQUENCY);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "MOPSO, " + trainingSet.get(0).name(),
            indicators.get(0).name(),
            indicators.get(1).name(),
            trainingSet.get(0).name(),
            PLOT_UPDATE_FREQUENCY);

    nsgaii.observable().register(evaluationObserver);
    nsgaii.observable().register(frontChartObserver);
    nsgaii.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    nsgaii.run();

    // Step 6: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + nsgaii.totalComputingTime());

    outputResults.updateEvaluations(META_MAX_EVALUATIONS);
    outputResults.writeResultsToFiles(nsgaii.result());

    System.exit(0);
  }
}
