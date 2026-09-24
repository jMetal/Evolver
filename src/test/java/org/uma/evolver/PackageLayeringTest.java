package org.uma.evolver;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the split between Evolver's configurable core and its meta level: the core packages
 * ({@code algorithm}, {@code parameter}, {@code util}) can be used on their own to configure and
 * run algorithms, so they must never reference the meta level ({@code meta}), the command-line
 * tools ({@code cli}), the irace integration ({@code irace}) or the examples ({@code example}).
 * The dependency only goes the other way.
 */
@DisplayName("Package layering between the configurable core and the meta level")
class PackageLayeringTest {

  private static final Path SOURCE_ROOT = Path.of("src/main/java/org/uma/evolver");

  private static final List<String> CORE_PACKAGES = List.of("algorithm", "parameter", "util");

  private static final Pattern FORBIDDEN_REFERENCE =
      Pattern.compile("org\\.uma\\.evolver\\.(meta|cli|irace|example)\\b[\\w.]*");

  @Test
  @DisplayName(
      "given the core packages' source files, when scanning them for references, then none points"
          + " to the meta level, the CLI, irace or the examples")
  void givenCoreSources_whenScanningReferences_thenNoneReachesTheMetaLevel() throws IOException {
    // Arrange
    List<Path> coreSources = new ArrayList<>();
    for (String corePackage : CORE_PACKAGES) {
      try (Stream<Path> files = Files.walk(SOURCE_ROOT.resolve(corePackage))) {
        files.filter(file -> file.toString().endsWith(".java")).forEach(coreSources::add);
      }
    }

    // Act
    List<String> violations = new ArrayList<>();
    for (Path source : coreSources) {
      Matcher matcher = FORBIDDEN_REFERENCE.matcher(Files.readString(source));
      while (matcher.find()) {
        violations.add(SOURCE_ROOT.relativize(source) + " -> " + matcher.group());
      }
    }

    // Assert
    assertTrue(!coreSources.isEmpty(), "No core source files found under " + SOURCE_ROOT);
    assertTrue(
        violations.isEmpty(),
        "Core packages must not depend on the meta level, the CLI, irace or the examples: "
            + violations);
  }
}
