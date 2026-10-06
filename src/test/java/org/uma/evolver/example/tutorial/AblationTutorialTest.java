package org.uma.evolver.example.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.ConfigurationVariants;
import org.uma.evolver.util.ConfigurationFileReader;

@DisplayName("Unit tests for class AblationTutorial")
class AblationTutorialTest {

  private static Map<String, String> configurations() throws IOException {
    String tuned =
        new ConfigurationFileReader(AblationTutorial.TUNED_CONFIGURATION_FILE).getConfiguration(1);
    String defaults =
        new ConfigurationFileReader(AblationTutorial.DEFAULT_CONFIGURATION_FILE)
            .getConfiguration(1);
    return AblationTutorial.configurations(tuned, defaults);
  }

  /** The names of the parameters whose values differ, or that only one of the two has. */
  private static Set<String> differences(String first, String second) {
    Map<String, String> a = ConfigurationVariants.parameterValues(first);
    Map<String, String> b = ConfigurationVariants.parameterValues(second);
    Set<String> names = new HashSet<>(a.keySet());
    names.addAll(b.keySet());
    names.removeIf(name -> a.containsKey(name) && a.get(name).equals(b.get(name)));
    return names;
  }

  @Test
  @DisplayName(
      "given the study, when its configurations are built, then there are the default, the tuned"
          + " one and a variant per component, in that order")
  void givenTheStudy_whenItsConfigurationsAreBuilt_thenThereIsOnePerComponent()
      throws IOException {
    // Act
    var configurations = configurations();

    // Assert
    assertEquals(AblationTutorial.COMPONENTS.size() + 2, configurations.size());
    assertEquals("Default", List.copyOf(configurations.keySet()).get(0));
    assertEquals("Tuned", List.copyOf(configurations.keySet()).get(configurations.size() - 1));
  }

  @Test
  @DisplayName(
      "given a variant, when it is compared with the tuned configuration, then it differs only in"
          + " its component and in the sub-parameters that the component activates")
  void givenAVariant_whenComparedWithTheTuned_thenItDiffersOnlyInItsComponent()
      throws IOException {
    // Arrange
    var configurations = configurations();
    String tuned = configurations.get("Tuned");

    // Act & Assert: every difference of the crossover variant belongs to the crossover
    Set<String> crossover = differences(tuned, configurations.get("DefaultCrossover"));
    assertTrue(crossover.contains("crossover"), crossover.toString());
    assertTrue(
        crossover.stream()
            .allMatch(
                name ->
                    name.startsWith("crossover")
                        || name.startsWith("sbx")
                        || name.startsWith("blxAlphaBeta")),
        crossover.toString());

    // Without the archive, the archive parameters go away and nothing else changes
    assertEquals(
        Set.of("algorithmResult", "populationSizeWithArchive", "archiveType"),
        differences(tuned, configurations.get("NoArchive")));
    assertEquals(
        Set.of("selectionTournamentSize"), differences(tuned, configurations.get("DefaultSelection")));
  }
}
