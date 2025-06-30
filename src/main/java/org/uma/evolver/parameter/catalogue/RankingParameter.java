package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.ranking.impl.StrengthRanking;

public class RankingParameter<S extends Solution<?>> extends CategoricalParameter {
  public RankingParameter(String name, List<String> validRankings) {
    super(name, validRankings);
  }

  public Ranking<S> getRanking() {
    return switch (value()) {
      case "dominanceRanking" -> new FastNonDominatedSortRanking<>();
      case "strengthRanking" -> new StrengthRanking<>();
      default -> throw new JMetalException("Ranking does not exist: " + name());
    };
  }
}
