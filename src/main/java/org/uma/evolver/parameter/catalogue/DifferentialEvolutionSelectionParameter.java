package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

/**
 * A categorical parameter representing selection strategies for Differential Evolution (DE) algorithms.
 * This parameter configures how parent solutions are selected for the DE crossover operation.
 *
 * <p>The selection strategy in DE is typically fixed to a tournament-like selection where
 * individuals are selected from the current population to participate in the differential mutation.
 *
 * @param <S> The type of solutions being evolved (typically DoubleSolution for DE)
 */
public class DifferentialEvolutionSelectionParameter<S extends Solution<?>> extends
    CategoricalParameter {
  public static final String DEFAULT_NAME = "selection";

  /**
   * Creates a new DifferentialEvolutionSelectionParameter with the specified selection strategies.
   * 
   * @param selectionStrategies A list of valid selection strategy names. For standard DE,
   *                          this typically contains just one strategy ("tournament").
   * @throws IllegalArgumentException if selectionStrategies is null or empty
   */
  public DifferentialEvolutionSelectionParameter(List<String> selectionStrategies) {
    super(DEFAULT_NAME, selectionStrategies);
  }

  /**
   * Creates and returns a Selection operator for Differential Evolution.
   * 
   * @param matingPoolSize The size of the mating pool
   * @param numberOfParentsToSelect The number of parents to select for each application of the operator
   * @param sequenceGenerator A sequence generator for creating random indices
   * @return A configured Selection operator for Differential Evolution
   * @throws JMetalException if the current value does not match any known selection strategy
   * @throws IllegalArgumentException if matingPoolSize or numberOfParentsToSelect are not positive,
   *                                  or if sequenceGenerator is null
   */
  public Selection<DoubleSolution> getParameter(int matingPoolSize, int numberOfParentsToSelect,
      SequenceGenerator<Integer> sequenceGenerator) {
    if ("tournament".equals(value())) {
      return new DifferentialEvolutionSelection(matingPoolSize, numberOfParentsToSelect, false,
          sequenceGenerator);
    } else {
      throw new JMetalException("Selection strategy does not exist: " + value());
    }
  }
  
  /**
   * Returns the name of this parameter.
   * 
   * @return The string "selection"
   */
  @Override
  public String name() {
    return "selection";
  }
}
