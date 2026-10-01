package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
