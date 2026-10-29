package org.uma.evolver.example.baselevel.constrained;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConstrainedFrontPlotObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.Tanaka;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Example of running NSGA-II with a constraint-handling technique on Tanaka with the
 * epsilon-constrained method.
 *
 * <p>The feasible region of Tanaka is non-convex and disconnected, and its Pareto front lies on the
 * boundary of that region. The epsilon-constrained method ({@code --constraintHandling
 * epsilonConstrained}) treats as feasible the solutions whose violation is below a threshold that
 * starts at a quantile of the violations of the initial population and decreases to zero during
 * the run, so that the search can approach the front from outside the feasible region. The rest of
 * the configuration is the default one of NSGA-II, and the parameter space is loaded from {@code
 * NSGAIIDoubleConstrained.yaml}. The result is written to {@code VAR.csv}/{@code FUN.csv}, and the
 * number of feasible solutions and the quality indicators of the feasible ones against the Tanaka
 * reference front are printed at the end of the run.
 */
public class ConstrainedNSGAIIForTanakaExample {

  private static final long PLOT_DELAY_MILLIS = 100;

  /**
   * Runs the example.
   *
   * @param args not used
   * @throws IOException if the reference front cannot be read
   */
  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new Tanaka();
    String referenceFrontFileName = "resources/referenceFronts/Tanaka.csv";

    String yamlParameterSpaceFile = "NSGAIIDoubleConstrained.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    String[] parameters =
        """
        --constraintHandling epsilonConstrained
        --epsilonInitialQuantile 0.2
        --epsilonExponent 2.0
        --epsilonDecayFraction 0.8
        --algorithmResult population
        --createInitialSolutions default
        --offspringPopulationSize 100
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --selection tournament
        --selectionTournamentSize 2
        """
            .split("\\s+");

    var baseNSGAII =
        new DoubleNSGAII(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseNSGAII.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaii = baseNSGAII.build();

    // Live plot of the population: feasible solutions in black, infeasible ones in red
    var plot =
        new ConstrainedFrontPlotObserver<DoubleSolution>(
            "NSGA-II with epsilonConstrained, Tanaka", "f1", "f2", 500);
    plot.setReferenceFront(VectorUtils.readVectors(referenceFrontFileName, ","));
    plot.delay(PLOT_DELAY_MILLIS);
    nsgaii.observable().register(plot);

    nsgaii.run();

    JMetalLogger.logger.info("Total execution time : " + nsgaii.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + nsgaii.numberOfEvaluations());

    new SolutionListOutput(nsgaii.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    var feasibleSolutions =
        nsgaii.result().stream().filter(ConstraintHandling::isFeasible).toList();
    JMetalLogger.logger.info(
        "Feasible solutions: " + feasibleSolutions.size() + " of " + nsgaii.result().size());
    if (!feasibleSolutions.isEmpty()) {
      QualityIndicatorUtils.printQualityIndicators(
          SolutionListUtils.getMatrixWithObjectiveValues(feasibleSolutions),
          VectorUtils.readVectors(referenceFrontFileName, ","));
    }
  }
}
