package org.uma.evolver.parameter.catalogue;

import java.util.List;

import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.perturbation.Perturbation;
import org.uma.jmetal.component.catalogue.pso.perturbation.impl.FrequencySelectionMutationBasedPerturbation;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different perturbation strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring perturbation mechanisms used to maintain diversity in PSO algorithms.
 * 
 * <p>The available perturbation strategies are:
 * <ul>
 *   <li>frequencySelectionMutationBasedPerturbation: Applies a mutation operator to particles
 *       at a specified frequency to maintain diversity in the swarm</li>
 * </ul>
 * 
 * <p>This perturbation mechanism helps prevent premature convergence by periodically introducing
 * diversity into the population through mutation operations.
 */
public class PerturbationParameter extends CategoricalParameter {
  /**
   * Creates a new PerturbationParameter with the specified valid values.
   * 
   * @param perturbationStrategies A list of valid perturbation strategy names.
   *                              Currently supports: "frequencySelectionMutationBasedPerturbation"
   * @throws IllegalArgumentException if perturbationStrategies is null or empty
   */
  public PerturbationParameter(List<String> perturbationStrategies) {
    super("perturbation", perturbationStrategies);
  }

  /**
   * Creates and returns a Perturbation instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>Required sub-parameters for the frequencySelectionMutationBasedPerturbation strategy:
   * <ul>
   *   <li>mutation: A MutationParameter<DoubleSolution> defining the mutation operator to apply</li>
   *   <li>frequencyOfApplicationOfMutationOperator: Integer specifying how often the perturbation is applied</li>
   * </ul>
   * 
   * @return A configured Perturbation implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known perturbation strategy
   * @throws ClassCastException if any required sub-parameter has an incorrect type
   * @throws NullPointerException if any required sub-parameter is not found
   */
  public Perturbation getPerturbation() {
    Perturbation result;

    if ("frequencySelectionMutationBasedPerturbation".equals(value())) {
      var mutationParameter = (MutationParameter<DoubleSolution>) findSpecificSubParameter("mutation");
      MutationOperator<DoubleSolution> mutationOperator = mutationParameter.getMutation();

      int frequencyOfApplication =
          (int) findSpecificSubParameter("frequencyOfApplicationOfMutationOperator").value();

      result =
          new FrequencySelectionMutationBasedPerturbation(mutationOperator, frequencyOfApplication);
    } else {
      throw new JMetalException("Perturbation component unknown: " + value());
    }

    return result;
  }

  /**
   * Returns the name of this parameter.
   * 
   * @return The string "perturbation"
   */
  @Override
  public String name() {
    return "perturbation";
  }
}
