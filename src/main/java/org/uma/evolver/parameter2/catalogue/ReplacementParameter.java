package org.uma.evolver.parameter2.catalogue;

import java.util.List;
import org.uma.evolver.parameter2.impl.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.KnnDensityEstimator;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.ranking.impl.StrengthRanking;

public class ReplacementParameter extends CategoricalParameter {
  public ReplacementParameter(List<String> selectionStrategies) {
    super("replacement", selectionStrategies);
  }

  public Replacement<?> getParameter() {
    String removalPolicy = (String) findGlobalParameter("removalPolicy").value();
    Replacement<?> result;
    switch (value()) {
      case "rankingAndDensityEstimatorReplacement":
        String rankingName = (String) findSpecificParameter("rankingForReplacement").value();
        String densityEstimatorName =
            (String) findSpecificParameter("densityEstimatorForReplacement").value();

        Ranking<Solution<?>> ranking;
        if (rankingName.equals("dominanceRanking")) {
          ranking = new FastNonDominatedSortRanking<>();
        } else {
          ranking = new StrengthRanking<>();
        }

        DensityEstimator<Solution<?>> densityEstimator;
        if (densityEstimatorName.equals("crowdingDistance")) {
          densityEstimator = new CrowdingDistanceDensityEstimator<>();
        } else {
          densityEstimator = new KnnDensityEstimator<>(1);
        }

        if (removalPolicy.equals("oneShot")) {
          result =
              new RankingAndDensityEstimatorReplacement<>(
                  ranking, densityEstimator, Replacement.RemovalPolicy.ONE_SHOT);
        } else {
          result =
              new RankingAndDensityEstimatorReplacement<>(
                  ranking, densityEstimator, Replacement.RemovalPolicy.SEQUENTIAL);
        }

        break;
      default:
        throw new JMetalException("Replacement component unknown: " + value());
    }

    return result;
  }
}
