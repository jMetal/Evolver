package org.uma.evolver.parameter.yaml;

import java.io.*;
import java.util.*;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.yaml.snakeyaml.Yaml;

/**
 * A simplified ParameterSpace implementation that loads its configuration from a YAML file. This
 * version only handles top-level parameters with basic types (categorical, integer, double).
 */
public class YAMLParameterSpace extends ParameterSpace {

  /**
   * Constructs a new YAMLParameterSpace by loading parameters from the specified YAML file.
   *
   * @param yamlFilePath Path to the YAML file containing parameter definitions
   */
  public YAMLParameterSpace(String yamlFilePath) {
    super();
    loadParametersFromYAML(yamlFilePath);
  }

  /**
   * Loads and processes parameters from the specified YAML file.
   *
   * @param yamlFilePath Path to the YAML file containing parameter definitions
   */
  private void loadParametersFromYAML(String yamlFilePath) {
    Map<String, Map<String, Object>> parameterDefinitions = loadParameterDefinitions(yamlFilePath);
    processParameterDefinitions(parameterDefinitions);
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
   * Processes a single parameter definition from the YAML configuration.
   *
   * @param parameterName Name of the parameter being processed
   * @param parameterConfig Map containing the parameter's configuration
   */
  @SuppressWarnings("unchecked")
  private void processParameterDefinition(String parameterName, Map<String, Object> parameterConfig) {
    if (!parameterConfig.containsKey("type") || !parameterConfig.containsKey("values")) {
      System.err.println("Skipping parameter " + parameterName + " - missing type or values");
      return;
    }

    String parameterType = ((String) parameterConfig.get("type")).toLowerCase();
    System.out.println("Processing parameter: " + parameterName + " (type: " + parameterType + ")");

    try {
      switch (parameterType) {
        case "categorical":
          processCategoricalParameter(parameterName, parameterConfig.get("values"));
          break;
        case "integer":
          processIntegerParameter(parameterName, parameterConfig.get("values"));
          break;
        case "double":
        case "real":
          processDoubleParameter(parameterName, parameterConfig.get("values"));
          break;
        default:
          System.err.println("Unsupported parameter type: " + parameterType);
      }
    } catch (Exception exception) {
      System.err.println("Error processing parameter " + parameterName + ": " + exception.getMessage());
    }
  }

  /**
   * Processes a categorical parameter definition.
   *
   * @param parameterName Name of the categorical parameter
   * @param parameterValues Object containing the parameter's possible values
   */
  @SuppressWarnings("unchecked")
  private void processCategoricalParameter(String parameterName, Object parameterValues) {
    List<String> stringCategories = new ArrayList<>();
    List<Integer> numericCategories = new ArrayList<>();

    // Handle array-style values: [val1, val2, ...]
    if (parameterValues instanceof List) {
      List<?> categoryList = (List<?>) parameterValues;

      if (categoryList.isEmpty()) {
        System.out.println("  - Creating empty categorical parameter");
        put(new CategoricalParameter(parameterName, stringCategories));
        return;
      }

      // Check if values are numeric
      if (categoryList.get(0) instanceof Number) {
        for (Object category : categoryList) {
          if (category instanceof Number) {
            numericCategories.add(((Number) category).intValue());
          }
        }
        System.out.println("  - Creating integer categorical parameter with values: " + numericCategories);
        put(new CategoricalIntegerParameter(parameterName, numericCategories));
      }
      // Handle string values
      else {
        for (Object category : categoryList) {
          stringCategories.add(category.toString());
        }
        System.out.println("  - Creating string categorical parameter with values: " + stringCategories);
        put(new CategoricalParameter(parameterName, stringCategories));
      }
    }
    // Handle map-style values: {value1: {...}, value2: {...}, ...}
    else if (parameterValues instanceof Map) {
      Map<String, Object> categoryMap = (Map<String, Object>) parameterValues;
      stringCategories.addAll(categoryMap.keySet());
      System.out.println("  - Creating categorical parameter from map keys: " + stringCategories);
      put(new CategoricalParameter(parameterName, stringCategories));
    } else {
      System.err.println("  - Unsupported values format for parameter " + parameterName);
    }
  }

  /**
   * Processes an integer parameter definition.
   *
   * @param parameterName Name of the integer parameter
   * @param parameterValues Object containing the parameter's value specification
   */
  @SuppressWarnings("unchecked")
  private void processIntegerParameter(String parameterName, Object parameterValues) {
    if (parameterValues instanceof List) {
      List<?> valueList = (List<?>) parameterValues;

      if (valueList.size() == 2 && valueList.get(0) instanceof Number) {
        // Treat as [min, max] range
        int minValue = ((Number) valueList.get(0)).intValue();
        int maxValue = ((Number) valueList.get(1)).intValue();
        System.out.println("  - Creating integer parameter with range: [" + minValue + ", " + maxValue + "]");
        put(new IntegerParameter(parameterName, minValue, maxValue));
      } else {
        // Handle as discrete values
        List<Integer> discreteValues = new ArrayList<>();
        for (Object value : valueList) {
          if (value instanceof Number) {
            discreteValues.add(((Number) value).intValue());
          }
        }
        if (!discreteValues.isEmpty()) {
          System.out.println("  - Creating integer categorical parameter with values: " + discreteValues);
          put(new CategoricalIntegerParameter(parameterName, discreteValues));
        } else {
          System.err.println("  - No valid integer values found for parameter " + parameterName);
        }
      }
    } else {
      System.err.println("  - Unsupported values format for integer parameter " + parameterName);
    }
  }

  /**
   * Processes a double/real parameter definition.
   *
   * @param parameterName Name of the double/real parameter
   * @param parameterValues Object containing the parameter's value specification
   */
  @SuppressWarnings("unchecked")
  private void processDoubleParameter(String parameterName, Object parameterValues) {
    if (parameterValues instanceof List) {
      List<?> valueList = (List<?>) parameterValues;

      if (valueList.size() == 2 && valueList.get(0) instanceof Number) {
        // Treat as [min, max] range
        double minValue = ((Number) valueList.get(0)).doubleValue();
        double maxValue = ((Number) valueList.get(1)).doubleValue();
        System.out.println("  - Creating double parameter with range: [" + minValue + ", " + maxValue + "]");
        put(new DoubleParameter(parameterName, minValue, maxValue));
      } else {
        // Handle as discrete values
        List<Double> discreteValues = new ArrayList<>();
        for (Object value : valueList) {
          if (value instanceof Number) {
            discreteValues.add(((Number) value).doubleValue());
          }
        }
        if (!discreteValues.isEmpty()) {
          System.out.println("  - Creating double categorical parameter with values: " + discreteValues);
          // Convert to strings for CategoricalParameter
          List<String> stringCategories = new ArrayList<>();
          for (Double value : discreteValues) {
            stringCategories.add(value.toString());
          }
          put(new CategoricalParameter(parameterName, stringCategories));
        } else {
          System.err.println("  - No valid double values found for parameter " + parameterName);
        }
      }
    } else {
      System.err.println("  - Unsupported values format for double parameter " + parameterName);
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
      System.err.println("Failed to load parameter space: " + error.getMessage());
      error.printStackTrace();
      System.exit(1);
    }
  }
}
