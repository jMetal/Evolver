package org.uma.evolver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@DisplayName("Unit tests for class ConfigurationFileReader")
class ConfigurationFileReaderTest {

  private static final String TEST_CONFIG_FILE = "defaultConfigurations/NSGAIIDoubleDefault.txt";

  @Nested
  @DisplayName("When constructing")
  class WhenConstructing {

    @Test
    @DisplayName("given a null file path, when constructing, then an IllegalArgumentException is thrown")
    void givenNullFilePath_whenConstructing_thenIllegalArgumentExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new ConfigurationFileReader(null);

      // Act & Assert
      assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    @DisplayName("given an empty file path, when constructing, then an IllegalArgumentException is thrown")
    void givenEmptyFilePath_whenConstructing_thenIllegalArgumentExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new ConfigurationFileReader("");

      // Act & Assert
      assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    @DisplayName("given a relative path, when constructing, then the file is loaded from src/main/resources")
    void givenRelativePath_whenConstructing_thenFileIsLoadedFromResources() throws IOException {
      // Arrange & Act
      ConfigurationFileReader reader = new ConfigurationFileReader(TEST_CONFIG_FILE);

      // Assert
      assertNotNull(reader.getConfiguration(1));
    }

    @Test
    @DisplayName("given an absolute path outside resources, when constructing, then the file is loaded from that path")
    void givenAbsolutePathOutsideResources_whenConstructing_thenFileIsLoadedFromOriginalPath()
        throws IOException {
      // Arrange
      Path tempFile = Files.createTempFile("test-config-", ".txt");
      Files.writeString(tempFile, "test configuration");

      // Act
      ConfigurationFileReader reader = new ConfigurationFileReader(tempFile.toString());

      // Assert
      assertEquals("test configuration", reader.getConfiguration(1).trim());
      Files.deleteIfExists(tempFile);
    }

    @Test
    @DisplayName("given a non-existent path, when constructing, then an IOException is thrown mentioning both locations")
    void givenNonExistentPath_whenConstructing_thenIOExceptionIsThrown() {
      // Arrange
      String nonExistentPath = "nonexistent/file/path.txt";
      Executable executable = () -> new ConfigurationFileReader(nonExistentPath);

      // Act & Assert
      IOException exception = assertThrows(IOException.class, executable);
      assertTrue(exception.getMessage().contains("src/main/resources/" + nonExistentPath));
      assertTrue(exception.getMessage().contains(nonExistentPath));
    }
  }

  @Nested
  @DisplayName("When reading configurations")
  class WhenReadingConfigurations {

    private ConfigurationFileReader configReader;

    @BeforeEach
    void setUp() throws IOException {
      Path resourcePath = Paths.get("src", "main", "resources", TEST_CONFIG_FILE);
      configReader = new ConfigurationFileReader(resourcePath.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("given a loaded config file, when getting the count, then it returns 1")
    void givenLoadedConfigFile_whenGettingCount_thenReturnsOne() {
      // Act
      int count = configReader.getNumberOfConfigurations();

      // Assert
      assertEquals(1, count);
    }

    @Test
    @DisplayName("given a loaded config file, when getting configuration 1, then it contains the expected entries")
    void givenLoadedConfigFile_whenGettingConfiguration1_thenContainsExpectedEntries() {
      // Act
      String config = configReader.getConfiguration(1);

      // Assert
      assertNotNull(config);
      assertFalse(config.isEmpty());
      assertTrue(config.contains("--algorithmResult population"));
      assertTrue(config.contains("--variation crossoverAndMutationVariation"));
      assertTrue(config.contains("--crossover SBX"));
      assertTrue(config.contains("--mutation polynomial"));
    }

    @Test
    @DisplayName("given a loaded config file, when getting all configurations, then the list matches getConfiguration(1)")
    void givenLoadedConfigFile_whenGettingAllConfigurations_thenListMatchesGetConfiguration1() {
      // Act
      List<String> allConfigs = configReader.getAllConfigurations();

      // Assert
      assertEquals(1, allConfigs.size());
      assertEquals(configReader.getConfiguration(1), allConfigs.get(0));
    }

    @Test
    @DisplayName("given a loaded config file, when accessing line 0, then an IndexOutOfBoundsException is thrown")
    void givenLoadedConfigFile_whenAccessingLine0_thenIndexOutOfBoundsExceptionIsThrown() {
      // Arrange
      Executable executable = () -> configReader.getConfiguration(0);

      // Act & Assert
      assertThrows(IndexOutOfBoundsException.class, executable);
    }

    @Test
    @DisplayName("given a loaded config file, when accessing a line beyond the last, then an IndexOutOfBoundsException is thrown")
    void givenLoadedConfigFile_whenAccessingLineBeyondLast_thenIndexOutOfBoundsExceptionIsThrown() {
      // Arrange
      Executable executable = () -> configReader.getConfiguration(2);

      // Act & Assert
      assertThrows(IndexOutOfBoundsException.class, executable);
    }
  }
}
