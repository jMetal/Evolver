package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.ranking.impl.StrengthRanking;

/**
 * A categorical parameter representing different ranking strategies for solutions in multi-objective optimization.
 * This parameter allows selecting and configuring how solutions are ranked based on their dominance relation
 * or other criteria.
 * 
 * <p>The available ranking strategies are:
 * <ul>
 *   <li>dominanceRanking: Ranks solutions based on non-dominated sorting (FastNonDominatedSort)</li>
 *   <li>strengthRanking: Ranks solutions based on strength values (used in SPEA2)</li>
 * </ul>
 * 
 * @param <S> The type of solutions being ranked
 */
public class RankingParameter<S extends Solution<?>> extends CategoricalParameter {
  /**
   * Creates a new RankingParameter with the specified name and valid ranking strategies.
   * 
   * @param name The name of the parameter
   * @param validRankings A list of valid ranking strategy names. Supported values:
   *                     - "dominanceRanking"
   *                     - "strengthRanking"
   * @throws IllegalArgumentException if name is null or empty, or if validRankings is null or empty
   */
  public RankingParameter(String name, List<String> validRankings) {
    super(name, validRankings);
  }

  /**
   * Creates and returns a Ranking instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   * 
   * @return A configured Ranking implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known ranking strategy
   */
  public Ranking<S> getRanking() {
    return switch (value()) {
      case "dominanceRanking" -> new FastNonDominatedSortRanking<>();
      case "strengthRanking" -> new StrengthRanking<>();
      default -> throw new JMetalException("Ranking does not exist: " + name());
    };
  }
  
  /**
   * Returns the name of this parameter.
   * 
   * @return The name of this parameter as specified in the constructor
   */
  @Override
  public String name() {
    return super.name();
  }
}
