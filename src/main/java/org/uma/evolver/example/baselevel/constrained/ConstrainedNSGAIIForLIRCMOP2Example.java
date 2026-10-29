package org.uma.evolver.example.baselevel.constrained;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
import org.uma.evolver.util.ConstrainedFrontPlotObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.lircmop.LIRCMOP2;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * NSGA-II with the constrained-domination principle on LIR-CMOP2, a problem with constraints whose
 * feasible region is small: its default configuration for problems with constraints ({@code
 * NSGAIIDoubleConstrainedDefault.txt}, {@code --constraintHandling cdp}), with the parameter space
 * {@code NSGAIIDoubleConstrained.yaml}.
 *
 * <p>The live plot shows the population, with the feasible solutions in black and the infeasible
 * ones in red: the population starts infeasible, and with seed 1 a third of it is feasible after
 * 2500 evaluations and all of it after 5000, close to the reference front (in gray).
 *
 * <p>The constraint-handling technique matters on this problem. With the same seed and budget,
 * NSGA-II with {@code NSGAIIDouble.yaml}, the parameter space for problems without constraints,
 * which ignores them, has no feasible solution in 50000 evaluations; with {@code
 * --constraintHandling epsilonConstrained --epsilonInitialQuantile 0.2 --epsilonExponent 2.0
 * --epsilonDecayFraction 0.8}, the population becomes feasible only after about 40000, when the
 * tolerance of the epsilon-constrained method has decreased. To try them, change the
 * configuration (and the space) below.
 *
 * <p>At the end it prints how many solutions of the result are feasible and the quality indicators
 * of the feasible ones with respect to the reference front.
 */
public class ConstrainedNSGAIIForLIRCMOP2Example {

  private static final int POPULATION_SIZE = 100;
  private static final int EVALUATIONS = 50000;
  private static final String REFERENCE_FRONT = "resources/referenceFronts/LIRCMOP2.csv";
  private static final int PLOT_UPDATE_FREQUENCY = 1000;
  private static final long PLOT_DELAY_MILLIS = 500;

  /**
   * Runs the example.
   *
   * @param args not used
   * @throws IOException if the default configuration or the reference front cannot be read
   */
  public static void main(String[] args) throws IOException {
    String configuration =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleConstrainedDefault.txt")
            .getConfiguration(1);

    JMetalRandom.getInstance().setSeed(1);
    var nsgaII =
        new DoubleNSGAII(
            new LIRCMOP2(),
            POPULATION_SIZE,
            EVALUATIONS,
            new YAMLParameterSpace("NSGAIIDoubleConstrained.yaml", new DoubleParameterFactory()));
    nsgaII.parse(configuration.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = nsgaII.build();

    // Live plot of the population: feasible solutions in black, infeasible ones in red
    var plot =
        new ConstrainedFrontPlotObserver<DoubleSolution>(
            "NSGA-II with cdp, LIR-CMOP2", "f1", "f2", PLOT_UPDATE_FREQUENCY);
    plot.setReferenceFront(VectorUtils.readVectors(REFERENCE_FRONT, ","));
    plot.delay(PLOT_DELAY_MILLIS);
    algorithm.observable().register(plot);

    algorithm.run();

    List<DoubleSolution> feasible =
        algorithm.result().stream().filter(ConstraintHandling::isFeasible).toList();

    JMetalLogger.logger.info("Time (ms): " + algorithm.totalComputingTime());
    JMetalLogger.logger.info(
        "Feasible solutions: " + feasible.size() + " of " + algorithm.result().size());
    if (!feasible.isEmpty()) {
      QualityIndicatorUtils.printQualityIndicators(
          SolutionListUtils.getMatrixWithObjectiveValues(feasible),
          VectorUtils.readVectors(REFERENCE_FRONT, ","));
    }
  }
}
