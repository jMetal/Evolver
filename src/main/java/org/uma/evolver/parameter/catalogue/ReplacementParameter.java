package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;

public class ReplacementParameter<S extends Solution<?>> extends CategoricalParameter {
  private Ranking<S> ranking ;
  private DensityEstimator<S> densityEstimator ;

  public ReplacementParameter(List<String> selectionStrategies) {
    super("replacement", selectionStrategies);
  }

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

  public void setRanking(Ranking<S> ranking) {
    this.ranking = ranking;
  }

  public void setDensityEstimator(DensityEstimator<S> densityEstimator) {
    this.densityEstimator = densityEstimator;
  }
  
}
