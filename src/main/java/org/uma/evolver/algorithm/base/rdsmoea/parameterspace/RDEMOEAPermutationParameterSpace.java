package org.uma.evolver.algorithm.base.rdsmoea.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsPermutationParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.PermutationCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.PermutationMutationParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;

/**
 * Parameter space for NSGA-II algorithm variants using permutation-based solutions.
 *
 * <p>This class extends {@link RDEMOEACommonParameterSpace} and adds parameters and relationships
 * specific to permutation problems, such as permutation crossover and mutation operators.
 *
 * <p>It defines and configures all relevant parameters for the NSGA-II algorithm when
 * working with {@code PermutationSolution<Integer>} representations.
 *
 * <p>Typical usage:
 * <pre>{@code
 * NSGAIIPermutationParameterSpace parameterSpace = new NSGAIIPermutationParameterSpace();
 * // Configure parameters as needed
 * }</pre>
 */
public class RDEMOEAPermutationParameterSpace extends RDEMOEACommonParameterSpace<PermutationSolution<Integer>> {
  public RDEMOEAPermutationParameterSpace() {
    super();
    setParameterSpace();
    setParameterRelationships();
    setTopLevelParameters();
  }

  
  // Initial solutions creation
  public static final String DEFAULT = "default";

  // Crossover
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";

  // Crossover strategies
  public static final String PMX = "PMX";
  public static final String OXD = "OXD";
  public static final String CX = "CX";

  // Mutation
  public static final String MUTATION_PROBABILITY = "mutationProbability";

  // Mutation strategies
  public static final String SWAP = "swap";
  public static final String INSERT = "insert";
  public static final String SCRAMBLE = "scramble";
  public static final String INVERSION = "inversion";
  public static final String SIMPLE_INVERSION = "simpleInversion";
  public static final String DISPLACEMENT = "displacement";

  /**
   * Defines and adds all parameters specific to permutation-based NSGA-II to the parameter space.
   * <p>
   * This method should call {@code super.setParameterSpace()} and then add any permutation-specific
   * parameters, such as permutation crossover and mutation operators.
   * </p>
   */
  @Override
  protected void setParameterSpace() {
    super.setParameterSpace();
    put(new CreateInitialSolutionsPermutationParameter(List.of(DEFAULT)));
    put(new PermutationCrossoverParameter(List.of(PMX, OXD, CX)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.6, 0.9));
    put(new PermutationMutationParameter(List.of(SWAP, INSERT, SCRAMBLE, INVERSION, SIMPLE_INVERSION, DISPLACEMENT)));
    put(new DoubleParameter(MUTATION_PROBABILITY, 0.05, 0.1));
  }

  /**
   * Establishes relationships and dependencies between parameters specific to permutation-based NSGA-II.
   * <p>
   * This method should call {@code super.setParameterRelationships()} and then add any
   * additional relationships for permutation-specific parameters.
   * </p>
   */
  @Override
  protected void setParameterRelationships() {
    super.setParameterRelationships();

    // Variation dependencies
    parameterSpace.get(CROSSOVER).addGlobalSubParameter(parameterSpace.get(CROSSOVER_PROBABILITY));
    parameterSpace.get(MUTATION).addGlobalSubParameter(parameterSpace.get(MUTATION_PROBABILITY));
  }
}
