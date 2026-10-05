package org.uma.evolver.parameter.yaml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Checks what the bundled base-level parameter spaces must not offer. A training samples the
 * components of a space at random, so a value that fails turns up in any long training: this is
 * the one found by running a training of every registered algorithm.
 */
@DisplayName("Unit tests of the content of the bundled parameter spaces")
class BundledParameterSpacesTest {

  private static final Path PARAMETER_SPACES = Path.of("src/main/resources/parameterSpaces");

  /** The values of a categorical parameter of a space, as a list or as a map. */
  private static Set<String> valuesOf(Path file, String parameter) throws IOException {
    @SuppressWarnings("unchecked")
    Map<String, Object> space = new Yaml().load(Files.newBufferedReader(file));
    @SuppressWarnings("unchecked")
    Map<String, Object> definition = (Map<String, Object>) space.get(parameter);
    if (definition == null) {
      return Set.of();
    }
    Object values = definition.get("values");
    if (values instanceof Map<?, ?> map) {
      return map.keySet().stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
    }
    return ((List<?>) values).stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
  }

  private static List<Path> spaces(String suffix) throws IOException {
    try (Stream<Path> files = Files.list(PARAMETER_SPACES)) {
      return files
          .filter(path -> path.getFileName().toString().endsWith(suffix + ".yaml"))
          .sorted()
          .toList();
    }
  }

  @Test
  @DisplayName(
      "given the spaces for binary and permutation problems, when read, then they only offer the"
          + " default initialization: sampling and scatter search need real variables")
  void givenBinaryAndPermutationSpaces_whenRead_thenOnlyTheDefaultInitializationIsOffered()
      throws IOException {
    // Arrange
    List<Path> files = new ArrayList<>(spaces("Binary"));
    files.addAll(spaces("Permutation"));

    // Act & Assert
    assertTrue(files.size() >= 8, "only " + files.size() + " spaces found");
    for (Path file : files) {
      Set<String> values = valuesOf(file, "createInitialSolutions");
      assertTrue(Set.of("default").containsAll(values), file.getFileName() + ": " + values);
    }
  }
}
