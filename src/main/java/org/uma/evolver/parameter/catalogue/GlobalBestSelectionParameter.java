package org.uma.evolver.parameter.catalogue;

import java.util.Comparator;
import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.pso.globalbestselection.GlobalBestSelection;
import org.uma.jmetal.component.catalogue.pso.globalbestselection.impl.NaryTournamentGlobalBestSelection;
import org.uma.jmetal.component.catalogue.pso.globalbestselection.impl.RandomGlobalBestSelection;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different global best selection strategies for Particle Swarm Optimization (PSO).
 * This parameter allows selecting and configuring how the global best solution is chosen from the swarm.
 * 
 * <p>The available global best selection strategies are:
 * <ul>
 *   <li>tournament: Selects the global best using a tournament selection mechanism</li>
 *   <li>random: Randomly selects a solution from the swarm as the global best</li>
 * </ul>
 * 
 * <p>The global best selection strategy influences the exploration-exploitation balance
 * of the PSO algorithm by determining how the best solution is propagated through the swarm.
 */
public class GlobalBestSelectionParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "globalBestSelection";
  
  /**
   * Creates a new GlobalBestSelectionParameter with the specified valid values.
   * 
   * @param selectionStrategies A list of valid global best selection strategy names.
   *                          Supported values: "tournament", "random"
   * @throws IllegalArgumentException if selectionStrategies is null or empty
   */
  public GlobalBestSelectionParameter(List<String> selectionStrategies) {
    super(DEFAULT_NAME, selectionStrategies);
  }

  /**
   * Creates and returns a GlobalBestSelection instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * <p>Required sub-parameters for each strategy:
   * <ul>
   *   <li>tournament: "selectionTournamentSize" (Integer)</li>
   *   <li>random: No additional parameters required</li>
   * </ul>
   * 
   * @param comparator The comparator used to compare solutions during selection. Must not be null.
   * @return A configured GlobalBestSelection implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known selection strategy
   * @throws ClassCastException if any required sub-parameter has an incorrect type
   * @throws NullPointerException if any required sub-parameter is not found or if comparator is null
   */
  public GlobalBestSelection getGlobalBestSelection(Comparator<DoubleSolution> comparator) {
    GlobalBestSelection result;
    switch (value()) {
      case "tournamentSelection" -> {
        int tournamentSize = (Integer) findConditionalSubParameter("selectionTournamentSize").value();

        result = new NaryTournamentGlobalBestSelection(tournamentSize, comparator);
      }
      case "randomSelection" -> result = new RandomGlobalBestSelection();
      default -> throw new JMetalException("Global Best Selection component unknown: " + value());
    }

    return result;
  }
  
  /**
   * Returns the name of this parameter.
   * 
   * @return The string "globalBestSelection"
   */
  @Override
  public String name() {
    return "globalBestSelection";
  }
}
