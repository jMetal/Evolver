package org.uma.evolver.algorithm.base.moead.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationDoubleParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Parameter space for the MOEA/D algorithm using real-coded (double) solutions.
 *
 * <p>This class defines all configurable parameters, their relationships, and top-level parameters
 * for the MOEA/D algorithm when applied to real-coded problems. It is designed to be flexible and
 * extensible, following the same structure as the NSGA-II and MOEA/D permutation parameter space classes.
 *
 * <p>Typical parameters include:
 * <ul>
 *   <li>Neighborhood size and replacement settings</li>
 *   <li>Aggregation function and normalization options</li>
 *   <li>Real-coded variation, crossover, and mutation operators</li>
 *   <li>Selection strategy and neighborhood selection probability</li>
 *   <li>Archive configuration and algorithm result type</li>
 *   <li>Initial solutions creation and sub-problem ID generator</li>
 * </ul>
 *
 * <p>Parameter relationships and dependencies are also defined, ensuring that the configuration is consistent and valid.
 * Subclasses can extend this class to add or specialize parameters for specific MOEA/D variants.
 *
 * <p>To configure the parameter space, override the methods {@link #setParameterSpace()},
 * {@link #setParameterRelationships()}, and {@link #setTopLevelParameters()} as needed.
 *
 * <p>Example of extension:
 * <pre>{@code
 * public class CustomMOEADDoubleParameterSpace extends MOEADDoubleParameterSpace {
 *   // Add custom parameters and relationships here
 * }
 * }</pre>
 *
 * @see MOEADCommonParameterSpace
 */
public class MOEADDoubleParameterSpace extends MOEADCommonParameterSpace<DoubleSolution> {

  // Mutation parameters
  public static final String MUTATION = "mutation";
  public static final String MUTATION_PROBABILITY_FACTOR = "mutationProbabilityFactor";
  public static final String MUTATION_REPAIR_STRATEGY = "mutationRepairStrategy";
  public static final String UNIFORM_MUTATION_PERTURBATION = "uniformMutationPerturbation";
  public static final String POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX = "polynomialMutationDistributionIndex";
  public static final String LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX = "linkedPolynomialMutationDistributionIndex";
  public static final String NON_UNIFORM_MUTATION_PERTURBATION = "nonUniformMutationPerturbation";

  // Crossover parameters
  public static final String CROSSOVER = "crossover";
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";
  public static final String CROSSOVER_REPAIR_STRATEGY = "crossoverRepairStrategy";
  public static final String SBX_DISTRIBUTION_INDEX = "sbxDistributionIndex";
  public static final String BLX_ALPHA_CROSSOVER_ALPHA_VALUE = "blxAlphaCrossoverAlphaValue";

  // Differential evolution parameters
  public static final String DIFFERENTIAL_EVOLUTION_CROSSOVER = "differentialEvolutionCrossover";
  public static final String CR = "CR";
  public static final String F = "F";

  // Variation values
  public static final String CROSSOVER_AND_MUTATION_VARIATION = "crossoverAndMutationVariation";
  public static final String DIFFERENTIAL_EVOLUTION_VARIATION = "differentialEvolutionVariation";

  // Mutation values
  public static final String UNIFORM = "uniform";
  public static final String POLYNOMIAL = "polynomial";
  public static final String LINKED_POLYNOMIAL = "linkedPolynomial";
  public static final String NON_UNIFORM = "nonUniform";

  // Crossover values
  public static final String SBX = "SBX";
  public static final String BLX_ALPHA = "BLX_ALPHA";
  public static final String WHOLE_ARITHMETIC = "wholeArithmetic";

  // Differential evolution crossover values
  public static final String RAND_1_BIN = "RAND_1_BIN";
  public static final String RAND_1_EXP = "RAND_1_EXP";
  public static final String RAND_2_BIN = "RAND_2_BIN";

  // Repair strategy values
  public static final String RANDOM = "random";
  public static final String ROUND = "round";
  public static final String BOUNDS = "bounds";

  // Initial solutions creation
  public static final String LATIN_HYPERCUBE_SAMPLING = "latinHypercubeSampling";
  public static final String SCATTER_SEARCH = "scatterSearch";

  @Override
  /**
   * Defines and adds all parameters to the parameter space for MOEA/D.
   * This includes neighborhood, aggregation, variation, mutation, crossover, selection, archive, and initialization parameters.
   */
  protected void setParameterSpace() {
    super.setParameterSpace();
    put(new CreateInitialSolutionsDoubleParameter(List.of(DEFAULT, LATIN_HYPERCUBE_SAMPLING, SCATTER_SEARCH)));
    put(new DoubleCrossoverParameter(List.of(SBX, BLX_ALPHA, WHOLE_ARITHMETIC)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.0, 1.0));
    put(new RepairDoubleSolutionStrategyParameter(CROSSOVER_REPAIR_STRATEGY, List.of(RANDOM, ROUND, BOUNDS)));
    put(new DoubleParameter(SBX_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(BLX_ALPHA_CROSSOVER_ALPHA_VALUE, 0.0, 1.0));
    put(new DoubleMutationParameter(List.of(UNIFORM, POLYNOMIAL, LINKED_POLYNOMIAL, NON_UNIFORM)));
    put(new DoubleParameter(MUTATION_PROBABILITY_FACTOR, 0.0, 2.0));
    put(new RepairDoubleSolutionStrategyParameter(MUTATION_REPAIR_STRATEGY, List.of(RANDOM, ROUND, BOUNDS)));
    put(new DoubleParameter(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DoubleParameter(NON_UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DifferentialEvolutionCrossoverParameter(List.of(RAND_1_BIN, RAND_1_EXP, RAND_2_BIN)));
    put(new DoubleParameter(CR, 0.0, 1.0));
    put(new DoubleParameter(F, 0.0, 1.0));
    put(new VariationDoubleParameter(List.of(CROSSOVER_AND_MUTATION_VARIATION, DIFFERENTIAL_EVOLUTION_VARIATION)));
  }

  @Override
  /**
   * Establishes relationships and dependencies between parameters in the MOEA/D parameter space.
   */
  protected void setParameterRelationships() {
    super.setParameterRelationships();
    // Variation dependencies
    get(CROSSOVER)
        .addGlobalSubParameter(get(CROSSOVER_PROBABILITY))
        .addGlobalSubParameter(get(CROSSOVER_REPAIR_STRATEGY))
        .addSpecificSubParameter(SBX, get(SBX_DISTRIBUTION_INDEX))
        .addSpecificSubParameter(BLX_ALPHA, get(BLX_ALPHA_CROSSOVER_ALPHA_VALUE));

    get(MUTATION)
        .addGlobalSubParameter(get(MUTATION_PROBABILITY_FACTOR))
        .addGlobalSubParameter(get(MUTATION_REPAIR_STRATEGY))
        .addSpecificSubParameter(UNIFORM, get(UNIFORM_MUTATION_PERTURBATION))
        .addSpecificSubParameter(NON_UNIFORM, get(NON_UNIFORM_MUTATION_PERTURBATION))
        .addSpecificSubParameter(POLYNOMIAL, get(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX))
        .addSpecificSubParameter(LINKED_POLYNOMIAL, get(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX));

    get(DIFFERENTIAL_EVOLUTION_CROSSOVER)
        .addGlobalSubParameter(get(CR))
        .addGlobalSubParameter(get(F));

    get(VARIATION)
        .addGlobalSubParameter(get(MUTATION))
        .addSpecificSubParameter(CROSSOVER_AND_MUTATION_VARIATION, get(CROSSOVER))
        .addSpecificSubParameter(DIFFERENTIAL_EVOLUTION_VARIATION, get(DIFFERENTIAL_EVOLUTION_CROSSOVER));
  }
}
