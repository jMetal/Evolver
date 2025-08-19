package org.uma.evolver.algorithm.base.rdsmoea.parameterspace;

import java.util.List;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.DoubleCrossoverParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.type.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/** Parameter space for RDS-MOEA with double-encoded solutions. */
public class RDEMOEADoubleParameterSpace extends RDEMOEACommonParameterSpace<DoubleSolution> {
  public RDEMOEADoubleParameterSpace() {
    super();
    setParameterSpace();
    setParameterRelationships();
    setTopLevelParameters();
  }

  @Override
  public RDEMOEADoubleParameterSpace createInstance() {
    return new RDEMOEADoubleParameterSpace();
  }

  // Initial solutions creation
  public static final String DEFAULT = "default";
  public static final String LATIN_HYPERCUBE_SAMPLING = "latinHypercubeSampling";
  public static final String SCATTER_SEARCH = "scatterSearch";

  // Crossover
  public static final String CROSSOVER_PROBABILITY = "crossoverProbability";
  public static final String CROSSOVER_REPAIR_STRATEGY = "crossoverRepairStrategy";

  // Crossover strategies
  public static final String SBX = "SBX";
  public static final String BLX_ALPHA = "blxAlpha";
  public static final String WHOLE_ARITHMETIC = "wholeArithmetic";
  public static final String SBX_DISTRIBUTION_INDEX = "sbxDistributionIndex";
  public static final String BLX_ALPHA_CROSSOVER_ALPHA_VALUE = "blxAlphaCrossoverAlphaValue";

  // Mutation
  public static final String MUTATION_PROBABILITY_FACTOR = "mutationProbabilityFactor";
  public static final String MUTATION_REPAIR_STRATEGY = "mutationRepairStrategy";

  // Mutation strategies
  public static final String UNIFORM = "uniform";
  public static final String POLYNOMIAL = "polynomial";
  public static final String LINKED_POLYNOMIAL = "linkedPolynomial";
  public static final String NON_UNIFORM = "nonUniform";
  public static final String POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX =
      "polynomialMutationDistributionIndex";
  public static final String LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX =
      "linkedPolynomialMutationDistributionIndex";
  public static final String UNIFORM_MUTATION_PERTURBATION = "uniformMutationPerturbation";
  public static final String NON_UNIFORM_MUTATION_PERTURBATION = "nonUniformMutationPerturbation";

  // Repair strategy values
  public static final String REPAIR_RANDOM = "random";
  public static final String REPAIR_ROUND = "round";
  public static final String REPAIR_BOUNDS = "bounds";

  @Override
  protected void setParameterSpace() {
    super.setParameterSpace();
    put(
        new CreateInitialSolutionsDoubleParameter(
            List.of(DEFAULT, LATIN_HYPERCUBE_SAMPLING, SCATTER_SEARCH)));

    put(new DoubleCrossoverParameter(List.of(SBX, BLX_ALPHA, WHOLE_ARITHMETIC)));
    put(new DoubleParameter(CROSSOVER_PROBABILITY, 0.0, 1.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            CROSSOVER_REPAIR_STRATEGY, List.of(REPAIR_RANDOM, REPAIR_ROUND, REPAIR_BOUNDS)));
    put(new DoubleParameter(SBX_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(BLX_ALPHA_CROSSOVER_ALPHA_VALUE, 0.0, 1.0));

    put(new DoubleMutationParameter(List.of(UNIFORM, POLYNOMIAL, LINKED_POLYNOMIAL, NON_UNIFORM)));
    put(new DoubleParameter(MUTATION_PROBABILITY_FACTOR, 0.0, 2.0));
    put(
        new RepairDoubleSolutionStrategyParameter(
            MUTATION_REPAIR_STRATEGY, List.of(REPAIR_RANDOM, REPAIR_ROUND, REPAIR_BOUNDS)));
    put(new DoubleParameter(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX, 5.0, 400.0));
    put(new DoubleParameter(UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
    put(new DoubleParameter(NON_UNIFORM_MUTATION_PERTURBATION, 0.0, 1.0));
  }

  @Override
  protected void setParameterRelationships() {
    super.setParameterRelationships();
    // Variation dependencies
    get(CROSSOVER)
        .addGlobalSubParameter(get(CROSSOVER_PROBABILITY))
        .addGlobalSubParameter(get(CROSSOVER_REPAIR_STRATEGY))
        .addConditionalParameter(SBX, get(SBX_DISTRIBUTION_INDEX))
        .addConditionalParameter(BLX_ALPHA, get(BLX_ALPHA_CROSSOVER_ALPHA_VALUE));

    get(MUTATION)
        .addGlobalSubParameter(get(MUTATION_PROBABILITY_FACTOR))
        .addGlobalSubParameter(get(MUTATION_REPAIR_STRATEGY))
        .addConditionalParameter(UNIFORM, get(UNIFORM_MUTATION_PERTURBATION))
        .addConditionalParameter(NON_UNIFORM, get(NON_UNIFORM_MUTATION_PERTURBATION))
        .addConditionalParameter(POLYNOMIAL, get(POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX))
        .addConditionalParameter(
            LINKED_POLYNOMIAL, get(LINKED_POLYNOMIAL_MUTATION_DISTRIBUTION_INDEX));
  }
}
