package org.uma.evolver.parameter.yaml;

import java.io.*;
import java.util.*;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.yaml.processors.*;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

/**
 * A simplified ParameterSpace implementation that loads its configuration from a YAML file. This
 * version only handles top-level parameters with basic types (categorical, integer, double).
 */
public class YAMLParameterSpace extends ParameterSpace {
  /** Map of parameter type names to their respective processors */
  private final Map<String, ParameterProcessor> parameterProcessors = new HashMap<>();

  /**
   * Constructs a new YAMLParameterSpace by loading parameters from the specified YAML file.
   *
   * @param yamlFilePath Path to the YAML file containing parameter definitions
   */
  public YAMLParameterSpace(String yamlFilePath) {
    super();
    initializeParameterProcessors();
    var parameterDefinitions = loadParametersFromYAML(yamlFilePath);
    processParameterDefinitions(parameterDefinitions);
  }

  /**
   * Initializes the map of parameter processors for different parameter types.
   */
  private void initializeParameterProcessors() {
    parameterProcessors.clear();
    parameterProcessors.put("categorical", new CategoricalParameterProcessor());
    parameterProcessors.put("integer", new IntegerParameterProcessor());
    parameterProcessors.put("int", new IntegerParameterProcessor());
    parameterProcessors.put("double", new DoubleParameterProcessor());
    parameterProcessors.put("real", new DoubleParameterProcessor());
  }
  
  /**
   * Gets the map of all parameter processors.
   *
   * @return A map of parameter type names to their respective processors
   */
  public Map<String, ParameterProcessor> getParameterProcessors() {
    return parameterProcessors;
  }
  
  /**
   * Gets the parameter processor for the specified parameter type.
   *
   * @param type The parameter type (e.g., "categorical", "integer", "double")
   * @return The parameter processor for the specified type, or null if not found
   */
  public ParameterProcessor getParameterProcessor(String type) {
    return parameterProcessors.get(type);
  }

  /**
   * Loads parameters from the specified YAML file.
   *
   * @param yamlFilePath Path to the YAML file containing parameter definitions
   */
  private Map<String, Map<String, Object>> loadParametersFromYAML(String yamlFilePath) {
    var parameterDefinitions = loadParameterDefinitions(yamlFilePath);

    return parameterDefinitions;    
  }

  /**
   * Loads parameter definitions from a YAML file.
   *
   * @param yamlFilePath Path to the YAML file
   * @return Map of parameter names to their configuration maps
   * @throws RuntimeException if there is an error reading or parsing the YAML file
   */
  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Object>> loadParameterDefinitions(String yamlFilePath) {
    try (InputStream configFileStream = openConfigFile(yamlFilePath)) {
      Yaml yamlParser = new Yaml();
      Map<String, Object> yamlContent = yamlParser.load(configFileStream);
      
      Map<String, Map<String, Object>> parameterDefinitions = new LinkedHashMap<>();
      
      for (Map.Entry<String, Object> entry : yamlContent.entrySet()) {
        if (entry.getValue() instanceof Map) {
          parameterDefinitions.put(entry.getKey(), (Map<String, Object>) entry.getValue());
        }
      }
      
      return parameterDefinitions;
    } catch (Exception exception) {
      throw new RuntimeException("Failed to load parameter definitions from YAML file: " + yamlFilePath, exception);
    }
  }
  
  /**
   * Processes a map of parameter definitions and adds them to the parameter space.
   *
   * @param parameterDefinitions Map of parameter names to their configuration maps
   */
  private void processParameterDefinitions(Map<String, Map<String, Object>> parameterDefinitions) {
    for (Map.Entry<String, Map<String, Object>> entry : parameterDefinitions.entrySet()) {
      String parameterName = entry.getKey();
      Map<String, Object> parameterConfig = entry.getValue();
      
      // Pass the entire parameter configuration to the processor
      // The processor will extract what it needs (type, values, globalSubParameters, etc.)
      processParameterDefinition(parameterName, parameterConfig);
    }
  }

  /**
   * Attempts to open the specified configuration file, first checking the classpath,
   * then falling back to the filesystem if not found.
   *
   * @param configFilePath Path to the configuration file
   * @return InputStream for reading the configuration file
   * @throws FileNotFoundException if the file cannot be found in either the classpath or filesystem
   */
  private InputStream openConfigFile(String configFilePath) throws FileNotFoundException {
    // First attempt to load from classpath
    InputStream configStream = getClass().getClassLoader().getResourceAsStream(configFilePath);

    // If not found in classpath, try filesystem
    if (configStream == null) {
      File configFile = new File(configFilePath);
      if (configFile.exists()) {
        configStream = new FileInputStream(configFile);
      } else {
        throw new FileNotFoundException(
            "Configuration file not found in classpath or filesystem: " + configFilePath);
      }
    }
    return configStream;
  }

  /**
   * Processes a parameter definition based on its type using the appropriate processor.
   *
   * @param parameterName Name of the parameter
   * @param parameterConfig Map containing parameter configuration
   */
  @SuppressWarnings("unchecked")
  private void processParameterDefinition(String parameterName, Map<String, Object> parameterConfig) {
    if (!parameterConfig.containsKey("type")) {
      throw new JMetalException("Skipping parameter " + parameterName + " - missing type");
    }

    String parameterType = ((String) parameterConfig.get("type")).toLowerCase();
    
    // For numeric types (integer, double), we allow either 'values' or 'range'
    // For categorical types, we require either 'values' or 'globalSubParameters'
    if (parameterType.equals("integer") || parameterType.equals("double") || parameterType.equals("real")) {
      if (!parameterConfig.containsKey("values") && !parameterConfig.containsKey("range")) {
        throw new JMetalException("Skipping parameter " + parameterName + " - missing 'values' or 'range' for " + parameterType + " type");
      }
    } else if (!parameterConfig.containsKey("values") && !parameterConfig.containsKey("globalSubParameters")) {
      throw new JMetalException("Skipping parameter " + parameterName + " - missing 'values' or 'globalSubParameters' for " + parameterType + " type");
    }

    System.out.println("Processing parameter: " + parameterName + " (type: " + parameterType + ")");

    ParameterProcessor processor = parameterProcessors.get(parameterType);
    if (processor != null) {
      // Pass the entire parameter config map instead of just the values
      // The processor will extract what it needs
      processor.process(parameterName, parameterConfig, this);
    } else {
      throw new JMetalException("Unsupported parameter type: " + parameterType);
    }
  }

  /**
   * Command-line entry point for testing the YAML parameter space loader.
   * 
   * @param args Command-line arguments (expects exactly one argument: path to YAML file)
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Usage: YAMLParameterSpace <path-to-yaml-config>");
      System.exit(1);
    }

    try {
      String configFilePath = args[0];
      System.out.println("Loading parameter space from: " + configFilePath);

      YAMLParameterSpace parameterSpace = new YAMLParameterSpace(configFilePath);
      int parameterCount = parameterSpace.parameters().size();
      System.out.println("\nSuccessfully loaded parameter space with " + parameterCount + " parameters:");

      // Display all loaded parameters
      for (String parameterName : parameterSpace.parameters().keySet()) {
        Parameter<?> parameter = parameterSpace.get(parameterName);
        System.out.println("  - " + parameterName + ": " + parameter);
      }

    } catch (Exception error) {
      throw new JMetalException("Failed to load parameter space: " + error.getMessage());
    }
  }
}
