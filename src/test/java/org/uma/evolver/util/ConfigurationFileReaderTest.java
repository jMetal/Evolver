package org.uma.evolver.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationFileReaderTest {

  private static final String TEST_CONFIG_FILE = "defaultConfigurations/NSGAIIDoubleDefault.txt";
  private ConfigurationFileReader configReader;

  @BeforeEach
  void setUp() throws IOException {
    // Get the absolute path to the test resource
    Path resourcePath = Paths.get("src", "main", "resources", TEST_CONFIG_FILE);
    String absolutePath = resourcePath.toAbsolutePath().toString();
    configReader = new ConfigurationFileReader(absolutePath);
  }

  @Test
  void shouldLoadConfigurationFileSuccessfully() {
    // Given: The setup method has created a ConfigurationFileReader
    // When: We check the number of configurations
    int count = configReader.getNumberOfConfigurations();

    // Then: The file should have exactly 1 configuration
    assertEquals(1, count, "Should have exactly one configuration");
  }

  @Test
  void shouldReturnCorrectConfiguration() {
    // Given: The configuration file has been loaded
    // When: We get the first configuration
    String config = configReader.getConfiguration(1);

    // Then: The configuration should match the expected format
    assertNotNull(config, "Configuration should not be null");
    assertFalse(config.isEmpty(), "Configuration should not be empty");

    // Verify some key parts of the configuration
    assertTrue(
        config.contains("--algorithmResult population"), "Should contain algorithm result setting");
    assertTrue(
        config.contains("--variation crossoverAndMutationVariation"),
        "Should contain variation setting");
    assertTrue(config.contains("--crossover SBX"), "Should contain crossover setting");
    assertTrue(config.contains("--mutation polynomial"), "Should contain mutation setting");
  }

  @Test
  void shouldReturnAllConfigurations() {
    // Given: The configuration file has been loaded
    // When: We get all configurations
    List<String> allConfigs = configReader.getAllConfigurations();

    // Then: We should get a list with exactly one configuration
    assertEquals(1, allConfigs.size(), "Should return exactly one configuration");

    // And: The configuration should match the one we get with getConfiguration(1)
    assertEquals(
        configReader.getConfiguration(1),
        allConfigs.get(0),
        "getAllConfigurations() should match getConfiguration(1)");
  }

  @Test
  void shouldThrowExceptionForInvalidLineNumber() {
    // Given: The configuration file has been loaded
    // When/Then: We try to access a non-existent line, it should throw IndexOutOfBoundsException
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> configReader.getConfiguration(0),
        "Should throw for line number 0");

    assertThrows(
        IndexOutOfBoundsException.class,
        () -> configReader.getConfiguration(2),
        "Should throw for line number 2");
  }

  @Test
  void shouldThrowExceptionForNullFilePath() {
    // When/Then: Creating with null file path should throw IllegalArgumentException
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConfigurationFileReader(null),
        "Should throw for null file path");
  }

  @Test
  void shouldThrowExceptionForEmptyFilePath() {
    // When/Then: Creating with empty file path should throw IllegalArgumentException
    assertThrows(
        IllegalArgumentException.class,
        () -> new ConfigurationFileReader(""),
        "Should throw for empty file path");
  }

  @Test
  void shouldLoadFileFromResourcesWhenNoAbsolutePathProvided() throws IOException {
    // Given: A configuration file that exists in src/main/resources
    String relativePath = TEST_CONFIG_FILE;
    
    // When: Creating a ConfigurationFileReader with just the filename
    ConfigurationFileReader reader = new ConfigurationFileReader(relativePath);
    
    // Then: The file should be loaded successfully from src/main/resources
    assertNotNull(reader.getConfiguration(1), "Should load configuration from src/main/resources");
  }
  
  @Test
  void shouldFallBackToOriginalPathWhenNotInResources() throws IOException {
    // Given: A temporary file that exists outside the resources directory
    Path tempFile = Files.createTempFile("test-config-", ".txt");
    Files.writeString(tempFile, "test configuration");
    
    // When: Creating a ConfigurationFileReader with the temp file path
    ConfigurationFileReader reader = new ConfigurationFileReader(tempFile.toString());
    
    // Then: The file should be loaded successfully from the original path
    assertEquals("test configuration", reader.getConfiguration(1).trim(), 
        "Should load configuration from the original path when not in resources");
    
    // Cleanup
    Files.deleteIfExists(tempFile);
  }
  
  @Test
  void shouldThrowExceptionForNonExistentFile() {
    // When/Then: Creating with non-existent file should throw IOException
    String nonExistentPath = "nonexistent/file/path.txt";
    IOException exception = assertThrows(
        IOException.class,
        () -> new ConfigurationFileReader(nonExistentPath),
        "Should throw for non-existent file");
        
    // Verify the error message contains both checked locations
    assertTrue(exception.getMessage().contains("src/main/resources/" + nonExistentPath), 
        "Error message should mention resources directory");
    assertTrue(exception.getMessage().contains(nonExistentPath), 
        "Error message should mention the original path");
  }
}
