package org.uma.evolver.example.baselevel.features;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.nsgaii.ConstrainedDoubleNSGAII;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.ConfigurationFileReader;
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
 * The two variants of NSGA-II on LIR-CMOP2, a problem with constraints: {@link DoubleNSGAII}, for
 * problems without constraints, which ignores them, and {@link ConstrainedDoubleNSGAII} with two
 * constraint-handling techniques of jMetal, the constrained-domination principle and the
 * epsilon-constrained method. All use the default configuration of NSGA-II and the same seed.
 *
 * <p>For each run it prints how many solutions of the result are feasible and, when there are
 * some, the quality indicators of the feasible ones with respect to the reference front.
 */
public class ConstrainedNSGAIILIRCMOP2Example {

  private static final int POPULATION_SIZE = 100;
  private static final int EVALUATIONS = 50000;
  private static final String REFERENCE_FRONT = "resources/referenceFronts/LIRCMOP2.csv";

  /**
   * Runs the example.
   *
   * @param args not used
   * @throws IOException if a default configuration or the reference front cannot be read
   */
  public static void main(String[] args) throws IOException {
    String constrainedDefault =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleConstrainedDefault.txt")
            .getConfiguration(1);
    String unconstrainedDefault =
        new ConfigurationFileReader("defaultConfigurations/NSGAIIDoubleDefault.txt")
            .getConfiguration(1);

    run(
        "NSGA-II (ignores the constraints)",
        new DoubleNSGAII(new LIRCMOP2(), POPULATION_SIZE, EVALUATIONS, space("NSGAIIDouble.yaml")),
        unconstrainedDefault);
    run(
        "Constrained NSGA-II, cdp",
        new ConstrainedDoubleNSGAII(
            new LIRCMOP2(), POPULATION_SIZE, EVALUATIONS, space("NSGAIIDoubleConstrained.yaml")),
        constrainedDefault);
    run(
        "Constrained NSGA-II, epsilonConstrained",
        new ConstrainedDoubleNSGAII(
            new LIRCMOP2(), POPULATION_SIZE, EVALUATIONS, space("NSGAIIDoubleConstrained.yaml")),
        constrainedDefault.replace(
            "--constraintHandling cdp",
            "--constraintHandling epsilonConstrained --epsilonInitialQuantile 0.2"
                + " --epsilonExponent 2.0 --epsilonDecayFraction 0.8"));
  }

  private static YAMLParameterSpace space(String file) {
    return new YAMLParameterSpace(file, new DoubleParameterFactory());
  }

  private static void run(String title, DoubleNSGAII nsgaII, String configuration)
      throws IOException {
    JMetalRandom.getInstance().setSeed(1);
    nsgaII.parse(configuration.split("\\s+"));
    EvolutionaryAlgorithm<DoubleSolution> algorithm = nsgaII.build();
    algorithm.run();

    List<DoubleSolution> feasible =
        algorithm.result().stream().filter(ConstraintHandling::isFeasible).toList();

    JMetalLogger.logger.info("==================================================");
    JMetalLogger.logger.info(title);
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
