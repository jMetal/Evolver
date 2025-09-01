package org.uma.evolver.algorithm.meta;

import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.SMPSOBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builder for creating SMPSO (Speed-constrained Multi-objective Particle Swarm Optimization)
 * instances configured for meta-optimization tasks. This builder provides a convenient way to
 * configure and instantiate SMPSO with appropriate settings for meta-optimization scenarios.
 *
 * <p>Key features:
 * <ul>
 *   <li>Configurable swarm size and maximum evaluations
 *   <li>Parallel evaluation support
 *   <li>Sensible defaults for meta-optimization tasks
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * MetaSMPSOBuilder builder = new MetaSMPSOBuilder(metaOptimizationProblem)
 *     .setSwarmSize(100)
 *     .setMaxEvaluations(10000)
 *     .setNumberOfCores(4);
 * ParticleSwarmOptimizationAlgorithm smpso = builder.build();
 * }</pre>
 *
 * @author [Your Name]
 * @see ParticleSwarmOptimizationAlgorithm
 * @see SMPSOBuilder
 * @see MetaOptimizationProblem
 */
public class MetaSMPSOBuilder {
  /** The problem to be solved */
  private final Problem<DoubleSolution> problem;

  /** The swarm size (default: 50) */
  private int swarmSize = 50;

  /** The maximum number of evaluations (default: 2000) */
  private int maxEvaluations = 2000;

  /** The number of cores to use for parallel evaluation (default: available processors) */
  private int numberOfCores = Runtime.getRuntime().availableProcessors();

  /**
   * Creates a new builder with the specified meta-optimization problem.
   *
   * @param problem the meta-optimization problem to be solved (must not be null)
   * @throws JMetalException if the problem is null
   */
  public MetaSMPSOBuilder(MetaOptimizationProblem<?> problem) {
    Check.notNull(problem);
    this.problem = problem;
  }

  /**
   * Sets the swarm size for the SMPSO algorithm.
   *
   * @param swarmSize the number of particles in the swarm (must be positive)
   * @return this builder instance for method chaining
   * @throws JMetalException if swarmSize is not positive
   */
  public MetaSMPSOBuilder setSwarmSize(int swarmSize) {
    Check.valueIsNotNegative(swarmSize);
    this.swarmSize = swarmSize;
    return this;
  }

  /**
   * Sets the maximum number of evaluations for the SMPSO algorithm.
   *
   * @param maxEvaluations the maximum number of function evaluations (must be positive)
   * @return this builder instance for method chaining
   * @throws JMetalException if maxEvaluations is not positive
   */
  public MetaSMPSOBuilder setMaxEvaluations(int maxEvaluations) {
    Check.valueIsNotNegative(maxEvaluations);
    this.maxEvaluations = maxEvaluations;
    return this;
  }

  /**
   * Sets the number of cores to use for parallel evaluation.
   *
   * @param numberOfCores the number of CPU cores to use (1 for sequential execution)
   * @return this builder instance for method chaining
   * @throws JMetalException if numberOfCores is not positive
   */
  public MetaSMPSOBuilder setNumberOfCores(int numberOfCores) {
    Check.that(numberOfCores > 0, "Number of cores must be positive");
    this.numberOfCores = numberOfCores;
    return this;
  }

  /**
   * Builds and configures an SMPSO instance with the specified parameters.
   *
   * <p>The returned SMPSO instance is configured with:
   * <ul>
   *   <li>Default velocity constraint handling
   *   <li>Multi-threaded evaluation
   *   <li>Specified termination condition
   *   <li>Configured swarm size
   * </ul>
   *
   * @return a fully configured SMPSO instance ready for execution
   * @throws JMetalException if the problem is not a DoubleProblem or configuration is invalid
   * @throws ClassCastException if the problem cannot be cast to DoubleProblem
   */
  public ParticleSwarmOptimizationAlgorithm build() {
    Check.that(problem instanceof DoubleProblem, "SMPSO requires a DoubleProblem");
    
    var evaluation = new MultiThreadedEvaluation<DoubleSolution>(numberOfCores, problem);
    Termination termination = new TerminationByEvaluations(maxEvaluations);

    return new SMPSOBuilder((DoubleProblem) problem, swarmSize)
        .setTermination(termination)
        .setEvaluation(evaluation)
        .build();
  }
}
