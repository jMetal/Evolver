package org.uma.evolver.parameter.catalogue;

import java.util.Comparator;
import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PopulationAndNeighborhoodSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.RandomSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.neighborhood.Neighborhood;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

/**
 * A categorical parameter representing different selection strategies for evolutionary algorithms.
 * This parameter allows selecting and configuring how parent solutions are chosen for reproduction.
 *
 * <p>The available selection strategies are:
 *
 * <ul>
 *   <li>tournament: Selects solutions based on tournament selection
 *   <li>random: Selects solutions uniformly at random
 *   <li>populationAndNeighborhoodMatingPoolSelection: Selects solutions based on both
 *       population-wide and neighborhood-based criteria
 * </ul>
 *
 * <p>Required sub-parameters depend on the selection strategy:
 *
 * <ul>
 *   <li>For "tournament":
 *       <ul>
 *         <li>selectionTournamentSize: The size of the tournament (Integer)
 *       </ul>
 *   <li>For "populationAndNeighborhoodMatingPoolSelection":
 *       <ul>
 *         <li>neighborhoodSelectionProbability: Probability of selecting from neighborhood (Double)
 *         <li>neighborhood: A Neighborhood instance (provided via non-configurable parameters)
 *         <li>subProblemIdGenerator: A SequenceGenerator for subproblem IDs (provided via
 *             non-configurable parameters)
 *       </ul>
 * </ul>
 *
 * @param <S> The type of solutions being evolved
 */
public class SelectionParameter<S extends Solution<?>> extends CategoricalParameter {

  /**
   * Creates a new SelectionParameter with the specified selection strategies.
   *
   * @param selectionStrategies A list of valid selection strategy names. Supported values: -
   *     "tournament" - "random" - "populationAndNeighborhoodMatingPoolSelection"
   * @throws IllegalArgumentException if selectionStrategies is null or empty
   */
  public SelectionParameter(List<String> selectionStrategies) {
    super("selection", selectionStrategies);
  }

  /**
   * Creates and returns a Selection operator based on the current parameter value. The specific
   * implementation and required parameters depend on the selection strategy.
   *
   * @param matingPoolSize The number of solutions to select
   * @param comparator The comparator used to compare solutions (used by some selection strategies)
   * @return A configured Selection operator
   * @throws JMetalException if the current value does not match any known selection strategy, or if
   *     required sub-parameters are missing or invalid
   * @throws IllegalArgumentException if matingPoolSize is not positive or comparator is null
   */
  public Selection<S> getSelection(int matingPoolSize, Comparator<S> comparator) {
    Selection<S> result;
    switch (value()) {
      case "tournament" -> {
        int tournamentSize = (Integer) findSpecificSubParameter("selectionTournamentSize").value();

        result = new NaryTournamentSelection<>(tournamentSize, matingPoolSize, comparator);
      }
      case "random" -> result = new RandomSelection<>(matingPoolSize);
      case "populationAndNeighborhoodMatingPoolSelection" -> {
        double neighborhoodSelectionProbability =
            (double) findSpecificSubParameter("neighborhoodSelectionProbability").value();
        var neighborhood = (Neighborhood<S>) nonConfigurableSubParameters().get("neighborhood");
        Check.notNull(neighborhood);

        var subProblemIdGenerator =
            (SequenceGenerator<Integer>)
                nonConfigurableSubParameters().get("subProblemIdGenerator");
        Check.notNull(subProblemIdGenerator);

        result =
            new PopulationAndNeighborhoodSelection<>(
                matingPoolSize,
                subProblemIdGenerator,
                neighborhood,
                neighborhoodSelectionProbability,
                false); // selectCurrentSolution is false to match the original behavior
      }
      default -> throw new JMetalException("Selection component unknown: " + value());
    }

    return result;
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
