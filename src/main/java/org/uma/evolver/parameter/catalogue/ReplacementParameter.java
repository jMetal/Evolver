package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;

/**
 * A categorical parameter representing different replacement strategies in evolutionary algorithms.
 * This parameter allows selecting and configuring how individuals in the population are replaced
 * by new offspring solutions during the evolutionary process.
 *
 * <p>The available replacement strategies are:
 * <ul>
 *   <li>rankingAndDensityEstimator: Uses a combination of ranking and density estimation
 *       to select which solutions to replace</li>
 * </ul>
 *
 * <p>For the "rankingAndDensityEstimator" strategy, the following sub-parameters are required:
 * <ul>
 *   <li>removalPolicy: Either "oneShot" or "sequential" to determine how solutions are removed</li>
 * </ul>
 *
 * <p>This parameter also requires setting a Ranking and DensityEstimator instance using the
 * appropriate setter methods before use.
 *
 * @param <S> The type of solutions being evolved
 */
public class ReplacementParameter<S extends Solution<?>> extends CategoricalParameter {
  private Ranking<S> ranking;
  private DensityEstimator<S> densityEstimator;

  /**
   * Creates a new ReplacementParameter with the specified replacement strategies.
   *
   * @param selectionStrategies A list of valid replacement strategy names. Currently supports:
   *                          - "rankingAndDensityEstimator"
   * @throws IllegalArgumentException if selectionStrategies is null or empty
   */
  public ReplacementParameter(List<String> selectionStrategies) {
    super("replacement", selectionStrategies);
  }

  /**
   * Creates and returns a Replacement instance based on the current parameter value.
   * The specific implementation is determined by the current value of this parameter.
   *
   * <p>For the "rankingAndDensityEstimator" strategy, this method requires:
   * <ul>
   *   <li>A Ranking instance (set via {@link #setRanking})</li>
   *   <li>A DensityEstimator instance (set via {@link #setDensityEstimator})</li>
   *   <li>A global sub-parameter named "removalPolicy" with value "oneShot" or "sequential"</li>
   * </ul>
   *
   * @return A configured Replacement implementation based on the current parameter value
   * @throws JMetalException if the current value does not match any known replacement strategy,
   *                         or if the required sub-parameters are not properly configured
   * @throws IllegalStateException if ranking or densityEstimator have not been set
   */
  public Replacement<S> getReplacement() {
    String removalPolicy = (String) findGlobalSubParameter("removalPolicy").value();
    Replacement<S> result;
    switch (value()) {
      case "rankingAndDensityEstimator" -> {
        if (removalPolicy.equals("oneShot")) {
          result =
              new RankingAndDensityEstimatorReplacement<>(
                  ranking, densityEstimator, Replacement.RemovalPolicy.ONE_SHOT);
        } else if (removalPolicy.equals("sequential")){
          result =
              new RankingAndDensityEstimatorReplacement<>(
                  ranking, densityEstimator, Replacement.RemovalPolicy.SEQUENTIAL);
        } else {
          throw new JMetalException("Removal policy unknown: " + removalPolicy) ;
        }
      }
      default -> throw new JMetalException("Replacement component unknown: " + value());
    }

    return result;
  }

  /**
   * Sets the ranking strategy to be used by the replacement operator.
   * This must be called before {@link #getReplacement()} if using a ranking-based strategy.
   *
   * @param ranking The ranking strategy to use
   * @throws IllegalArgumentException if ranking is null
   */
  public void setRanking(Ranking<S> ranking) {
    if (ranking == null) {
      throw new IllegalArgumentException("Ranking cannot be null");
    }
    this.ranking = ranking;
  }

  /**
   * Sets the density estimator to be used by the replacement operator.
   * This must be called before {@link #getReplacement()} if using a density-based strategy.
   *
   * @param densityEstimator The density estimator to use
   * @throws IllegalArgumentException if densityEstimator is null
   */
  public void setDensityEstimator(DensityEstimator<S> densityEstimator) {
    if (densityEstimator == null) {
      throw new IllegalArgumentException("Density estimator cannot be null");
    }
    this.densityEstimator = densityEstimator;
  }
  
  /**
   * Returns the name of this parameter.
   *
   * @return The string "replacement"
   */
  @Override
  public String name() {
    return "replacement";
  }
  
}
