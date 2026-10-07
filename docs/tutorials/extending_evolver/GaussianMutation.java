package org.uma.evolver.parameter.catalogue.mutationparameter;

import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.RepairDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Gaussian mutation: each variable is mutated with a probability, adding to it a number drawn from
 * a normal distribution with mean 0 and a standard deviation of {@code sigma} times the width of
 * the range of the variable. The result is repaired if it falls out of its bounds.
 */
public class GaussianMutation implements MutationOperator<DoubleSolution> {
  private final double mutationProbability;
  private final double sigma;
  private final RepairDoubleSolution solutionRepair;
  private final JMetalRandom random = JMetalRandom.getInstance();

  public GaussianMutation(
      double mutationProbability, double sigma, RepairDoubleSolution solutionRepair) {
    Check.probabilityIsValid(mutationProbability);
    Check.that(sigma > 0.0, "The standard deviation must be positive: " + sigma);
    Check.notNull(solutionRepair);
    this.mutationProbability = mutationProbability;
    this.sigma = sigma;
    this.solutionRepair = solutionRepair;
  }

  @Override
  public double mutationProbability() {
    return mutationProbability;
  }

  @Override
  public DoubleSolution execute(DoubleSolution solution) {
    Check.notNull(solution);
    for (int i = 0; i < solution.variables().size(); i++) {
      if (random.nextDouble() < mutationProbability) {
        Bounds<Double> bounds = solution.getBounds(i);
        double width = bounds.getUpperBound() - bounds.getLowerBound();
        double value = solution.variables().get(i) + nextGaussian() * sigma * width;
        solution
            .variables()
            .set(
                i,
                solutionRepair.repairSolutionVariableValue(
                    value, bounds.getLowerBound(), bounds.getUpperBound()));
      }
    }
    return solution;
  }

  /** A number of a standard normal distribution (Box-Muller), from the random generator of jMetal. */
  private double nextGaussian() {
    double u1 = 1.0 - random.nextDouble(); // in (0, 1], so that the logarithm is finite
    double u2 = random.nextDouble();
    return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
  }
}
