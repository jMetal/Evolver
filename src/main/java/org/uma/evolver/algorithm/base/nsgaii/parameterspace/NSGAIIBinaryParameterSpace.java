package org.uma.evolver.algorithm.base.nsgaii.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsBinaryParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.BinaryCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.BinaryMutationParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.solution.binarysolution.BinarySolution;

/**
 * Parameter space for NSGA-II algorithm variants using binary-coded solutions.
 *
 * <p>This class extends {@link NSGAIICommonParameterSpace} and adds parameters specific to
 * binary-coded problems, including bit-flip mutation and single-point crossover operators.
 *
 * <p>It defines and configures all relevant parameters for the NSGA-II algorithm when
 * working with {@code BinarySolution} representations.
 *
 * <p>Example usage:
 * <pre>{@code
 * NSGAIIBinaryParameterSpace parameterSpace = new NSGAIIBinaryParameterSpace();
 * // Configure parameters as needed
 * }</pre>
 *
 * @author Antonio J. Nebro
 */
public class NSGAIIBinaryParameterSpace extends NSGAIICommonParameterSpace<BinarySolution> {

  public NSGAIIBinaryParameterSpace() {
    super();
    setParameterSpace();
    setParameterRelationships();
    setTopLevelParameters();
  }

  // Crossover parameters
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";
  
  // Crossover operator names
  public static final String HUX_CROSSOVER = "HUX";
  public static final String UNIFORM_CROSSOVER = "uniform";
  public static final String SINGLE_POINT_CROSSOVER = "singlePoint";
  
  // Mutation parameters
  public static final String MUTATION_PROBABILITY_FACTOR = "mutationProbabilityFactor";

  // Mutation operator names
  public static final String BIT_FLIP_MUTATION = "bitFlip";

  /**
   * Defines and adds all parameters specific to binary-coded NSGA-II to the parameter space.
   *
   * <p>This method calls {@code super.setParameterSpace()} and then adds binary-specific
   * parameters, including:
   * <ul>
   *   <li>Single-point crossover with configurable probability
   *   <li>Bit-flip mutation with configurable probability factor
   *   <li>Number of bits in a solution (non-configurable, must be set before use)
   * </ul>
   */
  protected void setParameterSpace() {
    super.setParameterSpace();

    put(new CreateInitialSolutionsBinaryParameter(List.of("default")));
    put(new BinaryCrossoverParameter(List.of(HUX_CROSSOVER, UNIFORM_CROSSOVER, SINGLE_POINT_CROSSOVER)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.0, 1.0));
    put(new BinaryMutationParameter(List.of(BIT_FLIP_MUTATION)));
    put(new DoubleParameter(MUTATION_PROBABILITY_FACTOR, 0.0, 2.0));
  }

  /**
   * Establishes relationships and dependencies between parameters specific to binary-coded NSGA-II.
   *
   * <p>This method calls {@code super.setParameterRelationships()} and then adds relationships
   * for binary-specific parameters, connecting crossover and mutation operators with their
   * respective parameters.
   */
  protected void setParameterRelationships() {
    super.setParameterRelationships();

    // Crossover dependencies
    get(CROSSOVER)
        .addGlobalSubParameter(get(CROSSOVER_PROBABILITY));

    // Mutation dependencies
    get(MUTATION)
        .addGlobalSubParameter(get(MUTATION_PROBABILITY_FACTOR));
  }
}
