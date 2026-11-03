package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.knowm.xchart.XYSeries;
import org.uma.evolver.meta.problem.ConstrainedIndicatorValue;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.impl.DefaultDoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;

@DisplayName("Title of the live front plot of a training run")
class TrainingFrontPlotObserverTest {

  private static final String ALGORITHMS = "NSGA-II optimizing RVEA";

  @Test
  @DisplayName("Given a run bounded by evaluations, when building the title, then it shows the evaluations of the limit")
  void givenRunBoundedByEvaluationsWhenBuildingTitleThenItShowsEvaluationsOfTheLimit() {
    // Arrange
    var metaSearch = new FlatMetaSearchConfig("NSGA-II", 2000, 50, 8, List.of());

    // Act
    String title = TrainingFrontPlotObserver.title(ALGORITHMS, metaSearch, 500, 90_000L);

    // Assert
    assertEquals("NSGA-II optimizing RVEA. Evaluations: 500 of 2000", title);
  }

  @Test
  @DisplayName("Given a run bounded by time, when building the title, then it shows the minutes of the limit and the evaluations")
  void givenRunBoundedByTimeWhenBuildingTitleThenItShowsMinutesOfTheLimitAndEvaluations() {
    // Arrange
    var metaSearch = new FlatMetaSearchConfig("NSGA-II", 0, 60.0, 50, 8, List.of());

    // Act
    String title = TrainingFrontPlotObserver.title(ALGORITHMS, metaSearch, 500, 738_000L);

    // Assert
    assertEquals("NSGA-II optimizing RVEA. Time: 12.3 of 60 min (500 evaluations)", title);
  }

  @Test
  @DisplayName("Given a time limit with decimals, when building the title, then the limit keeps them")
  void givenTimeLimitWithDecimalsWhenBuildingTitleThenTheLimitKeepsThem() {
    // Arrange
    var metaSearch = new FlatMetaSearchConfig("NSGA-II", 0, 7.5, 50, 8, List.of());

    // Act
    String title = TrainingFrontPlotObserver.title(ALGORITHMS, metaSearch, 40, 30_000L);

    // Assert
    assertEquals("NSGA-II optimizing RVEA. Time: 0.5 of 7.5 min (40 evaluations)", title);
  }

  /** A configuration with two objectives and, if given, its failed runs. */
  private static DoubleSolution configuration(double f1, double f2, Integer failedRuns) {
    var solution = new DefaultDoubleSolution(List.of(Bounds.create(0.0, 1.0)), 2, 0);
    solution.objectives()[0] = f1;
    solution.objectives()[1] = f2;
    if (failedRuns != null) {
      solution.attributes().put(ConstrainedIndicatorValue.FAILED_RUNS, failedRuns);
    }
    return solution;
  }

  @Test
  @DisplayName(
      "Given configurations with and without failed runs, when classified, then only those with"
          + " failed runs are rejected")
  void givenConfigurationsWithAndWithoutFailedRunsWhenClassifiedThenOnlyThoseWithThemAreRejected() {
    // Act and Assert
    assertTrue(TrainingFrontPlotObserver.withoutFailedRuns(configuration(0.1, 0.2, 0)));
    assertFalse(TrainingFrontPlotObserver.withoutFailedRuns(configuration(6.1, 6.2, 1)));
    assertTrue(TrainingFrontPlotObserver.withoutFailedRuns(configuration(0.1, 0.2, null)));
  }

  @Test
  @DisplayName(
      "Given a training with constraints, when its configurations are plotted, then those with"
          + " failed runs are a series of their own")
  void givenATrainingWithConstraintsWhenPlottedThenTheConfigurationsWithFailedRunsAreApart() {
    // Arrange
    var metaSearch = new FlatMetaSearchConfig("NSGA-II", 2000, 50, 8, List.of());
    var observer =
        new TrainingFrontPlotObserver(metaSearch, "NSGA-II", "ICMOP", "EP", "DASCMOP5", 10, true);
    var configurations =
        List.of(configuration(0.1, 0.5, 0), configuration(0.5, 0.1, 0), configuration(6.2, 6.3, 1));

    // Act
    int withoutFailedRuns =
        observer
            .feasibilityChart()
            .plot(configurations, TrainingFrontPlotObserver::withoutFailedRuns, "title");

    // Assert
    assertEquals(2, withoutFailedRuns);
    var withFailedRuns =
        (XYSeries) observer.feasibilityChart().chart().getSeriesMap().get("With failed runs");
    assertArrayEquals(new double[] {6.2}, withFailedRuns.getXData());
  }

  @Test
  @DisplayName("Given a training without constraints, when created, then it plots as before")
  void givenATrainingWithoutConstraintsWhenCreatedThenItHasNoFeasibilityChart() {
    // Arrange
    var metaSearch = new FlatMetaSearchConfig("NSGA-II", 2000, 50, 8, List.of());

    // Act
    var observer = new TrainingFrontPlotObserver(metaSearch, "NSGA-II", "EP", "NHV", "ZDT1", 10);

    // Assert
    assertNull(observer.feasibilityChart());
  }
}
