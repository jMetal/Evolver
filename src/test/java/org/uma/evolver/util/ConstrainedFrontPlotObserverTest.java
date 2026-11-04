package org.uma.evolver.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYSeries;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

@DisplayName("Unit tests for the plot of the feasible and infeasible solutions of a population")
class ConstrainedFrontPlotObserverTest {

  private final Srinivas problem = new Srinivas();
  private ConstrainedFrontPlotObserver<DoubleSolution> observer;

  @BeforeEach
  void setUp() {
    observer = new ConstrainedFrontPlotObserver<>("NSGA-II, Srinivas", "f1", "f2", 100);
  }

  private DoubleSolution solution(double f1, double f2, double violation) {
    DoubleSolution solution = problem.createSolution();
    solution.objectives()[0] = f1;
    solution.objectives()[1] = f2;
    solution.constraints()[0] = -violation;
    solution.constraints()[1] = 0.0;
    return solution;
  }

  private double[] x(String series) {
    return ((XYSeries) observer.chart().getSeriesMap().get(series)).getXData();
  }

  @Nested
  @DisplayName("When a population is plotted")
  class Plot {

    @Test
    @DisplayName(
        "Given feasible and infeasible solutions, when plotted, then each goes to its series and the"
            + " title counts the feasible ones")
    void givenBothKinds_whenPlotted_thenEachGoesToItsSeries() {
      // Arrange
      var population = List.of(solution(1, 5, 0), solution(2, 4, 3), solution(3, 3, 0));

      // Act
      observer.plot(population, 2000);

      // Assert
      assertArrayEquals(new double[] {1, 3}, x(ConstrainedFrontPlotObserver.FEASIBLE));
      assertArrayEquals(new double[] {2}, x(ConstrainedFrontPlotObserver.INFEASIBLE));
      assertEquals(
          "NSGA-II, Srinivas. Evaluations: 2000. Feasible: 2 of 3", observer.chart().getTitle());
    }

    @Test
    @DisplayName(
        "Given only infeasible solutions, when plotted, then there is no feasible series yet")
    void givenOnlyInfeasibleSolutions_whenPlotted_thenThereIsNoFeasibleSeries() {
      // Act
      observer.plot(List.of(solution(1, 5, 2), solution(2, 4, 3)), 100);

      // Assert
      assertFalse(
          observer.chart().getSeriesMap().containsKey(ConstrainedFrontPlotObserver.FEASIBLE));
      assertEquals(2, x(ConstrainedFrontPlotObserver.INFEASIBLE).length);
    }

    @Test
    @DisplayName(
        "Given a population that becomes feasible, when plotted again, then the infeasible series"
            + " is emptied")
    void givenAPopulationThatBecomesFeasible_whenPlottedAgain_thenTheInfeasibleSeriesIsEmptied() {
      // Arrange
      observer.plot(List.of(solution(1, 5, 2), solution(2, 4, 0)), 100);

      // Act
      observer.plot(List.of(solution(1, 5, 0), solution(2, 4, 0)), 200);

      // Assert
      assertEquals(0, x(ConstrainedFrontPlotObserver.INFEASIBLE).length);
      assertEquals(2, x(ConstrainedFrontPlotObserver.FEASIBLE).length);
    }

    @Test
    @DisplayName("Given a reference front, when set, then it is a series of its own")
    void givenAReferenceFront_whenSet_thenItIsASeriesOfItsOwn() {
      // Act
      observer.setReferenceFront(new double[][] {{0, 1}, {1, 0}});

      // Assert
      assertTrue(
          observer.chart().getSeriesMap().containsKey(ConstrainedFrontPlotObserver.REFERENCE_FRONT));
      assertArrayEquals(new double[] {0, 1}, x(ConstrainedFrontPlotObserver.REFERENCE_FRONT));
    }
  }

  @Test
  @DisplayName("Given an update frequency of 0, when created, then it is rejected")
  void givenAFrequencyOfZero_whenCreated_thenItIsRejected() {
    // Act & Assert
    assertThrows(
        InvalidConditionException.class,
        () -> new ConstrainedFrontPlotObserver<DoubleSolution>("t", "f1", "f2", 0));
  }
}
