package org.uma.evolver.example;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.gde3.GDE3Builder;
import org.uma.jmetal.algorithm.multiobjective.spea2.SPEA2Builder;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.operator.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.MultiThreadedSolutionListEvaluator;

/**
 * Class for running GDE3 as meta-optimizer to configure {@link ConfigurableNSGAII} using
 * problem {@link ZDT1} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class GDE3OptimizingNSGAIIForProblemZDT1 {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem (ZDT1)
    var indicators = List.of(new Epsilon(), new Spread());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT1();
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableNSGAII})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 15000);
    var configurableProblem = new MetaOptimizationProblem(configurableAlgorithm,
        referenceFrontFileName,
        indicators, 1);

    // Step 3: Set the parameters for the meta-optimizer (GDE3)
    double cr = 0.5;
    double f = 0.5;
    var crossover =
        new DifferentialEvolutionCrossover(
            cr, f, DifferentialEvolutionCrossover.DE_VARIANT.RAND_1_BIN);
    var selection = new DifferentialEvolutionSelection();

    int maxEvaluations = 2000;
    SolutionListEvaluator<DoubleSolution> evaluator =
        new MultiThreadedSolutionListEvaluator<DoubleSolution>(8);

    var gde3 =
        new GDE3Builder(configurableProblem)
            .setCrossover(crossover)
            .setSelection(selection)
            .setMaxEvaluations(maxEvaluations)
            .setPopulationSize(50)
            .setSolutionSetEvaluator(evaluator)
            .build();

    // Step 4: Run the meta-optimizer
    AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(gde3).execute();

    // Step 5: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + algorithmRunner.getComputingTime());

    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "NSGA-II", configurableProblem, problemWhoseConfigurationIsSearchedFor.name(), indicators,
        "RESULTS/NSGA-II/ZDT1");
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(gde3.result());

    System.exit(0);
  }
}
