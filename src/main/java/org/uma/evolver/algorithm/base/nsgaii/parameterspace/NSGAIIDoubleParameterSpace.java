package org.uma.evolver.algorithm.base.nsgaii.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Parameter space for NSGA-II algorithm variants using real-coded (double) solutions.
 *
 * <p>This class extends {@link NSGAIICommonParameterSpace} and adds parameters and relationships
 * specific to real-coded problems, such as real-valued crossover and mutation operators.
 *
 * <p>It defines and configures all relevant parameters for the NSGA-II algorithm when
 * working with {@code DoubleSolution} representations.
 *
 * <p>Typical usage:
 * <pre>{@code
 * NSGAIIDoubleParameterSpace parameterSpace = new NSGAIIDoubleParameterSpace();
 * // Configure parameters as needed
 * }</pre>
 */
public class NSGAIIDoubleParameterSpace extends NSGAIICommonParameterSpace<DoubleSolution> {

  // Initial solutions creation
  public static final String DEFAULT = "default";
  public static final String LATIN_HYPERCUBE_SAMPLING = "latinHypercubeSampling";
  public static final String SCATTER_SEARCH = "scatterSearch";

  // Crossover
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";
  public static final String CROSSOVER_REPAIR_STRATEGY = "crossoverRepairStrategy";

  // Crossover strategies
  public static final String SBX = "SBX";
  public static final String PCX = "PCX";
  public static final String BLX_ALPHA = "blxAlpha";
  public static final String WHOLE_ARITHMETIC = "wholeArithmetic";
  public static final String BLX_ALPHA_BETA = "blxAlphaBeta";
  public static final String ARITHMETIC = "arithmetic";
  public static final String LAPLACE = "laplace";
  public static final String FUZZY_RECOMBINATION = "fuzzyRecombination";
  public static final String UNDC = "UNDC";
  
  public static final String POWER_LAW_MUTATION_DELTA = "powerLawMutationDelta";
  public static final String SBX_DISTRIBUTION_INDEX = "sbxDistributionIndex";
  public static final String PCX_CROSSOVER_ZETA = "pcxCrossoverZeta";
  public static final String PCX_CROSSOVER_ETA = "pcxCrossoverEta";
  public static final String BLX_ALPHA_CROSSOVER_ALPHA = "blxAlphaCrossoverAlpha";
  public static final String BLX_ALPHA_BETA_CROSSOVER_BETA = "blxAlphaBetaCrossoverBeta";
  public static final String BLX_ALPHA_BETA_CROSSOVER_ALPHA = "blxAlphaBetaCrossoverAlpha";
  public static final String LAPLACE_CROSSOVER_SCALE = "laplaceCrossoverScale";
  public static final String FUZZY_RECOMBINATION_CROSSOVER_ALPHA = "fuzzyRecombinationCrossoverAlpha";
  public static final String UNDC_CROSSOVER_ZETA = "undcCrossoverZeta";
  public static final String UNDC_CROSSOVER_ETA = "undcCrossoverEta";

  // Mutation
  public static final String MUTATION_PROBABILITY_FACTOR = "mutationProbabilityFactor";
  public static final String MUTATION_REPAIR_STRATEGY = "mutationRepairStrategy";

  // Mutation strategies
  public static final String UNIFORM = "uniform";
  public static final String POLYNOMIAL = "polynomial";
  public static final String LINKED_POLYNOMIAL = "linkedPolynomial";
  public static final String NON_UNIFORM = "nonUniform";
  public static final String LEVY_FLIGHT = "levyFlight";
  public static final String POWER_LAW = "powerLaw";
  public static final String POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX =
      "polynomialMutationDistributionIndex";
  public static final String LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX =
      "linkedPolynomialMutationDistributionIndex";
  public static final String UNIFORM_MUTATION_PERTURBATION = "uniformMutationPerturbation";
  public static final String NON_UNIFORM_MUTATION_PERTURBATION = "nonUniformMutationPerturbation";
  public static final String LEVY_FLIGHT_MUTATION_BETA = "levyFlightMutationBeta";
  public static final String LEVY_FLIGHT_MUTATION_STEP_SIZE = "levyFlightMutationStepSize";

  // Repair strategy values
  public static final String REPAIR_RANDOM = "random";
  public static final String REPAIR_ROUND = "round";
  public static final String REPAIR_BOUNDS = "bounds";

  /**
   * Defines and adds all parameters specific to real-coded NSGA-II to the parameter space.
   * <p>
   * This method should call {@code super.setParameterSpace()} and then add any double-specific
   * parameters, such as real-valued crossover and mutation operators.
   * </p>
   */
  @Override
  protected void setParameterSpace() {
    super.setParameterSpace();
    put(
        new CreateInitialSolutionsDoubleParameter(
            List.of(DEFAULT, LATIN_HYPERCUBE_SAMPLING, SCATTER_SEARCH)));

    put(new DoubleCrossoverParameter(List.of(SBX, BLX_ALPHA, WHOLE_ARITHMETIC, BLX_ALPHA_BETA, ARITHMETIC, LAPLACE, FUZZY_RECOMBINATION, PCX)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.0, 1.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            CROSSOVER_REPAIR_STRATEGY, List.of(REPAIR_RANDOM, REPAIR_ROUND, REPAIR_BOUNDS)));
    put(new DoubleParameter(SBX_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(PCX_CROSSOVER_ZETA, 0.1, 0.5));
    put(new DoubleParameter(PCX_CROSSOVER_ETA, 0.1, 0.5));
    put(new DoubleParameter(BLX_ALPHA_CROSSOVER_ALPHA, 0.0, 1.0));
    put(new DoubleParameter(BLX_ALPHA_BETA_CROSSOVER_BETA, 0.0, 1.0));
    put(new DoubleParameter(BLX_ALPHA_BETA_CROSSOVER_ALPHA, 0.0, 1.0));
    put(new DoubleParameter(LAPLACE_CROSSOVER_SCALE, 0.0, 1.0));
    put(new DoubleParameter(FUZZY_RECOMBINATION_CROSSOVER_ALPHA, 0.0, 1.0));
    put(new DoubleParameter(UNDC_CROSSOVER_ZETA, 0.1, 1.0));
    put(new DoubleParameter(UNDC_CROSSOVER_ETA, 0.1, 0.5));
    put(new DoubleMutationParameter(List.of(UNIFORM, POLYNOMIAL, LINKED_POLYNOMIAL, NON_UNIFORM, LEVY_FLIGHT, POWER_LAW)));
    put(new DoubleParameter(MUTATION_PROBABILITY_FACTOR, 0.0, 2.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            MUTATION_REPAIR_STRATEGY, List.of(REPAIR_RANDOM, REPAIR_ROUND, REPAIR_BOUNDS)));
    put(new DoubleParameter(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DoubleParameter(NON_UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DoubleParameter(LEVY_FLIGHT_MUTATION_BETA, 1.0, 2.0));
    put(new DoubleParameter(LEVY_FLIGHT_MUTATION_STEP_SIZE, 0.01, 1.0));
    put(new DoubleParameter(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(POWER_LAW_MUTATION_DELTA, 0.0, 10.0));
  }

  /**
   * Establishes relationships and dependencies between parameters specific to real-coded NSGA-II.
   * <p>
   * This method should call {@code super.setParameterRelationships()} and then add any
   * additional relationships for double-specific parameters.
   * </p>
   */
  @Override
  protected void setParameterRelationships() {
    super.setParameterRelationships();
    // Variation dependencies
    get(CROSSOVER)
        .addGlobalSubParameter(get(CROSSOVER_PROBABILITY))
        .addGlobalSubParameter(get(CROSSOVER_REPAIR_STRATEGY))
        .addSpecificSubParameter(SBX, get(SBX_DISTRIBUTION_INDEX))
        .addSpecificSubParameter(PCX, get(PCX_CROSSOVER_ZETA))
        .addSpecificSubParameter(PCX, get(PCX_CROSSOVER_ETA))
        .addSpecificSubParameter(BLX_ALPHA, get(BLX_ALPHA_CROSSOVER_ALPHA))
        .addSpecificSubParameter(BLX_ALPHA_BETA, get(BLX_ALPHA_BETA_CROSSOVER_BETA))
        .addSpecificSubParameter(BLX_ALPHA_BETA, get(BLX_ALPHA_BETA_CROSSOVER_ALPHA))
        .addSpecificSubParameter(LAPLACE, get(LAPLACE_CROSSOVER_SCALE))
        .addSpecificSubParameter(FUZZY_RECOMBINATION, get(FUZZY_RECOMBINATION_CROSSOVER_ALPHA))
        .addSpecificSubParameter(UNDC, get(UNDC_CROSSOVER_ZETA))
        .addSpecificSubParameter(UNDC, get(UNDC_CROSSOVER_ETA));

    get(MUTATION)
        .addGlobalSubParameter(get(MUTATION_PROBABILITY_FACTOR))
        .addGlobalSubParameter(get(MUTATION_REPAIR_STRATEGY))
        .addSpecificSubParameter(UNIFORM, get(UNIFORM_MUTATION_PERTURBATION))
        .addSpecificSubParameter(NON_UNIFORM, get(NON_UNIFORM_MUTATION_PERTURBATION))
        .addSpecificSubParameter(POLYNOMIAL, get(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX))
        .addSpecificSubParameter(
            LINKED_POLYNOMIAL, get(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX))
        .addSpecificSubParameter(LEVY_FLIGHT, get(LEVY_FLIGHT_MUTATION_BETA))
        .addSpecificSubParameter(LEVY_FLIGHT, get(LEVY_FLIGHT_MUTATION_STEP_SIZE))
        .addSpecificSubParameter(POWER_LAW, get(POWER_LAW_MUTATION_DELTA)) ;
  }
}
