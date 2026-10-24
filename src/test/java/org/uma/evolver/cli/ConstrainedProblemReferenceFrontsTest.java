package org.uma.evolver.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The reference fronts of the registered problems with constraints, as Evolver-Studio finds them:
 * by the name of the problem in {@code resources/referenceFronts} ({@code NAME.csv} or {@code
 * NAME.xD.csv}), with as many comma-separated columns in their first line as objectives.
 */
@DisplayName("Unit tests for the reference fronts of the problems with constraints")
class ConstrainedProblemReferenceFrontsTest {

  private static final Path FRONTS = Path.of("resources/referenceFronts");

  /** The problems with constraints that jMetal 7.9 has no reference front for. */
  private static final Set<String> WITHOUT_FRONT = Set.of("Viennet4", "RCM17");

  static Stream<ProblemDescriptor> constrainedProblems() {
    return ProblemRegistry.registeredProblems().stream()
        .filter(problem -> problem.numberOfConstraints() != null)
        .filter(problem -> problem.numberOfConstraints() > 0)
        .filter(problem -> !WITHOUT_FRONT.contains(problem.name()));
  }

  private static List<Path> frontsOf(String name) throws IOException {
    Pattern file = Pattern.compile(Pattern.quote(name) + "(\\.\\d+D)?\\.csv");
    try (Stream<Path> files = Files.list(FRONTS)) {
      return files.filter(path -> file.matcher(path.getFileName().toString()).matches()).toList();
    }
  }

  /** The columns of the first line, counted as Evolver-Studio counts them. */
  private static int columnsOf(Path front) throws IOException {
    try (Stream<String> lines = Files.lines(front)) {
      return lines.findFirst().orElse("").strip().split(",", -1).length;
    }
  }

  @Test
  @DisplayName("the catalogue has the problems with constraints of jMetal")
  void givenTheCatalogue_whenFiltered_thenItHasTheConstrainedProblems() {
    // Act
    long count = constrainedProblems().count();

    // Assert: the 128 problems with constraints of jMetal 7.9 but SimpleIntegerProblem (integer
    // encoding), Viennet4 and RCM17 (no reference front)
    assertEquals(125, count);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("constrainedProblems")
  @DisplayName("every problem with constraints has a front with as many columns as objectives")
  void givenAConstrainedProblem_whenItsFrontsAreFound_thenOneMatchesItsObjectives(
      ProblemDescriptor problem) throws IOException {
    // Act
    List<Path> fronts = frontsOf(problem.name());

    // Assert
    assertFalse(fronts.isEmpty(), "No reference front for " + problem.name());
    boolean matching = false;
    for (Path front : fronts) {
      matching |= columnsOf(front) == problem.numberOfObjectives();
    }
    assertTrue(
        matching,
        problem.name() + " has " + problem.numberOfObjectives() + " objectives, fronts " + fronts);
  }
}
