package org.uma.evolver.parameter.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.constrainthandling.ConstrainedDominance;
import org.uma.jmetal.util.constrainthandling.StochasticRanking;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.ranking.impl.StrengthRanking;

@DisplayName("Unit tests for the ranking parameter")
class RankingParameterTest {

  private static RankingParameter<DoubleSolution> parsed(String ranking) {
    var parameter =
        new RankingParameter<DoubleSolution>(List.of("dominanceRanking", "strengthRanking"));
    parameter.parse(new String[] {"--ranking", ranking});
    return parameter;
  }

  /** A solution with two objectives and one constraint (negative when violated). */
  private static DoubleSolution solution(double f1, double f2, double constraint) {
    var solution = new DefaultDoubleSolution(List.of(Bounds.create(0.0, 1.0)), 2, 1);
    solution.objectives()[0] = f1;
    solution.objectives()[1] = f2;
    solution.constraints()[0] = constraint;
    return solution;
  }

  @Nested
  @DisplayName("When the ranking is created without a constraint-handling technique")
  class WithoutATechnique {

    @Test
    @DisplayName("dominanceRanking is the fast non-dominated sorting and strengthRanking SPEA2's")
    void givenEachValue_whenCreated_thenItIsTheUsualRanking() {
      // Act and Assert
      assertInstanceOf(FastNonDominatedSortRanking.class, parsed("dominanceRanking").getRanking());
      assertInstanceOf(StrengthRanking.class, parsed("strengthRanking").getRanking());
    }
  }

  @Nested
  @DisplayName("When the ranking is created with a constraint-handling technique")
  class WithATechnique {

    @Test
    @DisplayName("dominanceRanking is the ranking of the technique")
    void givenDominanceRanking_whenCreated_thenItIsTheRankingOfTheTechnique() {
      // Arrange
      var technique = new StochasticRanking<DoubleSolution>(0.45);

      // Act
      Ranking<DoubleSolution> ranking = parsed("dominanceRanking").getRanking(technique);

      // Assert
      assertEquals(technique.ranking().getClass(), ranking.getClass());
    }

    @Test
    @DisplayName(
        "strengthRanking compares with the technique: a feasible solution ranks before an"
            + " infeasible one that dominates it")
    void givenStrengthRanking_whenRanked_thenTheFeasibleSolutionComesFirst() {
      // Arrange
      var feasible = solution(1.0, 1.0, 0.0);
      var infeasibleButBetter = solution(0.0, 0.0, -1.0);
      Ranking<DoubleSolution> ranking =
          parsed("strengthRanking").getRanking(new ConstrainedDominance<>());

      // Act
      ranking.compute(List.of(feasible, infeasibleButBetter));

      // Assert
      assertInstanceOf(StrengthRanking.class, ranking);
      assertEquals(0, ranking.getRank(feasible));
      assertEquals(1, ranking.getRank(infeasibleButBetter));
    }
  }
}
