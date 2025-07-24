package org.uma.evolver.parameter.yaml;

import java.io.*;
import java.util.*;

import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.yaml.processors.*;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

/**
 * A YAML-based parameter space implementation that loads multi-objective metaheuristic
 * parameter configurations from YAML files.
 *
 * <p>This class extends {@link ParameterSpace} to provide dynamic parameter loading
 * capabilities from YAML configuration files. It supports various parameter types
 * including categorical, integer, and double parameters, with full support for
 * nested and conditional parameter structures commonly found in metaheuristic
 * algorithm configurations.
 *
 * @author Antonio J. Nebro
 * @version 1.0
 * @since 1.0
 * @notThreadSafe This class is not thread-safe. External synchronization should be used if
 *                instances are accessed from multiple threads.
 *
 * <p>The class uses a processor pattern to handle different parameter types,
 * making it easily extensible for new parameter types. Each parameter type
 * is processed by a specialized {@link ParameterProcessor} that understands
 * the specific configuration requirements and validation rules.
 *
 * <p><strong>Supported Parameter Types:</strong>
 * <ul>
 * <li><strong>Categorical:</strong> Parameters with discrete string values</li>
 * <li><strong>Integer/Int:</strong> Parameters with integer values (supports both discrete values and ranges).
 *     Both 'integer' and 'int' type names are accepted.</li>
 * <li><strong>Double/Real:</strong> Parameters with floating-point values (supports both discrete values and ranges).
 *     Both 'double' and 'real' type names are accepted and treated equivalently.</li>
 * </ul>
 *
 * <p><strong>Supported Parameter Structures:</strong>
 * <ul>
 * <li><strong>Top-level parameters:</strong> Standard parameters defined at the root level</li>
 * <li><strong>Conditional parameters:</strong> Parameters that are only active when parent parameters meet specific conditions</li>
 * <li><strong>Global sub-parameters:</strong> Parameters that are globally available as sub-parameters</li>
 * <li><strong>Nested parameters:</strong> Parameters defined within categorical parameter values</li>
 * </ul>
 *
 * <p><strong>Example YAML Configuration:</strong>
 * <pre>{@code
 * algorithmType:
 *   type: categorical
 *   values:
 *     basic: {}
 *     advanced:
 *       conditionalParameters:
 *         populationSize:
 *           type: integer
 *           range: [10, 100]
 *         crossoverRate:
 *           type: double
 *           range: [0.1, 0.9]
 *
 * mutationProbability:
 *   type: double
 *   values: [0.01, 0.05, 0.1]
 * }</pre>
 *
 * <p><strong>File Loading:</strong>
 * The class supports loading YAML files from both the classpath and the filesystem.
 * It first attempts to load from the classpath, then falls back to the filesystem
 * if the file is not found in the classpath.
 *
 * <p><strong>Error Handling:</strong>
 * The class provides comprehensive validation and error reporting through {@link JMetalException} for:
 * <ul>
 * <li>Missing required parameter configuration keys (e.g., 'type', 'values', or 'range')</li>
 * <li>Unsupported or unknown parameter types</li>
 * <li>Malformed YAML structures or invalid configurations</li>
 * <li>File not found errors when loading from classpath or filesystem</li>
 * <li>Invalid parameter values or ranges (e.g., min > max in range definitions)</li>
 * <li>Type mismatches in configuration values</li>
 * </ul>
 *
 * <p><strong>Implementation Notes:</strong>
 * <ul>
 * <li>Uses a processor pattern to handle different parameter types, making it easily extensible</li>
 * <li>Processes parameters in the order they appear in the YAML file</li>
 * <li>Validates all parameter configurations during loading</li>
 * <li>Supports loading from both classpath and filesystem with classpath having precedence</li>
 * </ul>
 *
 * <p><strong>Performance Considerations:</strong>
 * <ul>
 * <li>Loading large YAML files may impact startup time</li>
 * <li>Parameter validation is performed during loading, not during access</li>
 * <li>Consider caching the parameter space if used frequently</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Load parameter space from YAML file
 * YAMLParameterSpace parameterSpace = new YAMLParameterSpace("config/algorithms.yaml");
 *
 * // Access loaded parameters
 * Parameter<?> algorithmType = parameterSpace.get("algorithmType");
 *
 * // Get all parameters
 * Map<String, Parameter<?>> allParameters = parameterSpace.parameters();
 * }</pre>
 *
 * @see ParameterSpace
 * @see ParameterProcessor
 * @see Parameter
 * @see JMetalException
 *
 * @implSpec This implementation processes parameters in the order they appear in the YAML file.
 *           Parameters with the same name will override previous definitions.
 * @implNote The YAML parsing is handled by the SnakeYAML library, and this class expects
 *           the YAML to be well-formed according to the documented structure.
 * @author Antonio J. Nebro
 * @version 1.0
 * @notThreadSafe This class is not thread-safe. External synchronization should be used if
 *                instances are accessed from multiple threads.
 */
public class YAMLParameterSpace extends ParameterSpace {

  // Parameter type constants
  /** Constant for categorical parameter type */
  private static final String CATEGORICAL_TYPE = "categorical";
  /** Constant for integer parameter type */
  private static final String INTEGER_TYPE = "integer";
  /** Constant for integer parameter type (alternative) */
  private static final String INT_TYPE = "int";
  /** Constant for double parameter type */
  private static final String DOUBLE_TYPE = "double";
  /** Constant for real parameter type (alias for double) */
  private static final String REAL_TYPE = "real";

  // Parameter configuration key constants
  /** Key for parameter type in YAML configuration */
  private static final String TYPE_KEY = "type";
  /** Key for parameter values in YAML configuration */
  private static final String VALUES_KEY = "values";
  /** Key for parameter range in YAML configuration */
  private static final String RANGE_KEY = "range";
  /** Key for conditional parameters in YAML configuration */
  private static final String CONDITIONAL_PARAMETERS_KEY = "conditionalParameters";
  /** Key for global sub-parameters in YAML configuration */
  private static final String GLOBAL_SUB_PARAMETERS_KEY = "globalSubParameters";

  /** Map of parameter type names to their respective processors */
  private final Map<String, ParameterProcessor> parameterProcessors = new HashMap<>();

  private final ParameterFactory<?> parameterFactory ;
  private final String yamlFilePath ;

  /**
   * Constructs a new YAMLParameterSpace by loading parameters from the specified YAML file.
   *
   * <p>This constructor initializes the parameter processors, loads the YAML configuration,
   *
   * @param yamlFilePath the path to the YAML configuration file. The path can be
   *                    a classpath resource (e.g., "config/parameters.yaml") or a
   *                    filesystem path (e.g., "/path/to/parameters.yaml").
   * @param parameterFactory the factory to use for creating parameter instances.
   *                        This allows customization of parameter creation while
   *                        maintaining the YAML parsing logic.
   * @throws JMetalException if there is an error loading or parsing the YAML file,
   *                        or if the configuration is invalid.
   * @throws IllegalArgumentException if yamlFilePath is null or empty, or if
   *                                 parameterFactory is null.
   * @see #loadParametersFromYAML(String)
   * @see #processParameterDefinitions(Map)
   */
  public YAMLParameterSpace(String yamlFilePath, ParameterFactory<?> parameterFactory) {
    super();
    this.parameterFactory = parameterFactory ;
    this.yamlFilePath = yamlFilePath ;
    initializeParameterProcessors();
    var parameterDefinitions = loadParametersFromYAML(yamlFilePath);
    processParameterDefinitions(parameterDefinitions);
  }

  @Override
  public YAMLParameterSpace createInstance() {
    return new YAMLParameterSpace(yamlFilePath, parameterFactory);
  } 

  /**
   * Initializes the map of parameter processors for different parameter types.
   *
   * <p>This method registers processors for all supported parameter types:
   * <ul>
   * <li>Categorical parameters (discrete string values)</li>
   * <li>Integer parameters (discrete values or ranges)</li>
   * <li>Double/Real parameters (discrete values or ranges)</li>
   * </ul>
   */
  private void initializeParameterProcessors() {
    parameterProcessors.clear();
    parameterProcessors.put(CATEGORICAL_TYPE, new CategoricalParameterProcessor(parameterFactory));
    parameterProcessors.put(INTEGER_TYPE, new IntegerParameterProcessor());
    parameterProcessors.put(INT_TYPE, new IntegerParameterProcessor());
    parameterProcessors.put(DOUBLE_TYPE, new DoubleParameterProcessor());
    parameterProcessors.put(REAL_TYPE, new DoubleParameterProcessor());
  }

  /**
   * Returns an unmodifiable view of all parameter processors.
   *
   * <p>This method provides access to the internal processor registry for
   * introspection and testing purposes. The returned map contains mappings
   * from parameter type names to their corresponding processors.
   *
   * @return an unmodifiable map of parameter type names to their respective processors;
   *         never null
   */
  public Map<String, ParameterProcessor> getParameterProcessors() {
    return Collections.unmodifiableMap(parameterProcessors);
  }

  /**
   * Returns the parameter processor for the specified parameter type.
   *
   * <p>This method allows retrieval of specific processors for custom parameter
   * processing or validation logic.
   *
   * @param type the parameter type (e.g., "categorical", "integer", "double");
   *             case-sensitive
   * @return the parameter processor for the specified type, or {@code null} if not found
   */
  public ParameterProcessor getParameterProcessor(String type) {
    return parameterProcessors.get(type);
  }

  /**
   * Loads parameter definitions from the specified YAML file.
   *
   * <p>This method handles the file loading process and delegates to
   * {@link #loadParameterDefinitions(String)} for the actual parsing.
   *
   * @param yamlFilePath path to the YAML file containing parameter definitions
   * @return a map of parameter names to their configuration maps
   *
   * @throws RuntimeException if the file cannot be loaded or parsed
   */
  private Map<String, Map<String, Object>> loadParametersFromYAML(String yamlFilePath) {
    var parameterDefinitions = loadParameterDefinitions(yamlFilePath);
    return parameterDefinitions;
  }

  /**
   * Loads and parses parameter definitions from a YAML file.
   *
   * <p>This method attempts to load the file from the classpath first, then from
   * the filesystem if not found. It parses the YAML content and extracts parameter
   * definitions, filtering out any non-map entries.
   *
   * @param yamlFilePath path to the YAML file
   * @return a LinkedHashMap of parameter names to their configuration maps,
   *         preserving the order from the YAML file
   *
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
   * <p>This method handles both top-level parameters and recursively processes
   * any nested parameter structures including conditional parameters and
   * global sub-parameters.
   *
   * @param parameterDefinitions map of parameter names to their configuration maps
   */
  private void processParameterDefinitions(Map<String, Map<String, Object>> parameterDefinitions) {
    for (Map.Entry<String, Map<String, Object>> entry : parameterDefinitions.entrySet()) {
      String parameterName = entry.getKey();
      Map<String, Object> parameterConfig = entry.getValue();

      // Process the current parameter
      processParameterDefinition(parameterName, parameterConfig);

      // Add the parameter to the top level list of parameters
      addTopLevelParameter(get(parameterName));
    }
  }

  /**
   * Attempts to open the specified configuration file.
   *
   * <p>This method first attempts to load the file from the classpath using
   * the class loader, then falls back to the filesystem if the file is not
   * found in the classpath. This approach allows for flexible deployment
   * where configuration files can be packaged with the application or
   * provided externally.
   *
   * @param configFilePath path to the configuration file (relative to resources/parameterSpaces/)
   * @return an InputStream for reading the configuration file
   *
   * @throws FileNotFoundException if the file cannot be found in either
   *                               the classpath or filesystem
   */
  private InputStream openConfigFile(String configFilePath) throws FileNotFoundException {
    // Normalize the path to ensure consistent handling of path separators
    String normalizedPath = configFilePath.replace('\\', '/');
    
    // First attempt to load from classpath
    // Try with the path as-is first
    InputStream configStream = getClass().getClassLoader().getResourceAsStream(normalizedPath);
    
    // If not found, try prepending 'parameterSpaces/' if not already present
    if (configStream == null && !normalizedPath.contains("parameterSpaces/")) {
      String resourcePath = "parameterSpaces/" + normalizedPath;
      configStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
    }

    // If still not found in classpath, try filesystem as a fallback
    if (configStream == null) {
      File configFile = new File(configFilePath);
      if (configFile.exists()) {
        try {
          configStream = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
          throw new FileNotFoundException(String.format(
              "Failed to load configuration file from filesystem: %s. Error: %s", 
              configFile.getAbsolutePath(), e.getMessage()));
        }
      } else {
        // Try to find the file in the standard Maven resources directory
        File resourceFile = new File("src/main/resources/parameterSpaces/" + normalizedPath);
        if (resourceFile.exists()) {
          try {
            configStream = new FileInputStream(resourceFile);
          } catch (FileNotFoundException e) {
            throw new FileNotFoundException(String.format(
                "Configuration file not found in classpath, filesystem, or resources directory. " +
                "Tried: '%s', '%s', and '%s'.", 
                normalizedPath, configFile.getAbsolutePath(), resourceFile.getAbsolutePath()));
          }
        } else {
          throw new FileNotFoundException(String.format(
              "Configuration file not found in classpath, filesystem, or resources directory. " +
              "Tried: '%s', '%s', and '%s'.", 
              normalizedPath, configFile.getAbsolutePath(), resourceFile.getAbsolutePath()));
        }
      }
    }
    
    return configStream;
  }

  /**
   * Processes a single parameter definition using the appropriate processor.
   *
   * <p>This method performs validation of the parameter configuration,
   * determines the appropriate processor based on the parameter type,
   * and delegates the actual parameter creation to the processor.
   *
   * <p>The method validates that:
   * <ul>
   * <li>The parameter configuration contains a valid type</li>
   * <li>Numeric parameters (integer/double) have either 'values' or 'range' specified</li>
   * <li>Non-numeric parameters have either 'values' or 'globalSubParameters' specified</li>
   * </ul>
   *
   * @param parameterName the name of the parameter to process
   * @param parameterConfig the configuration map containing parameter settings
   *
   * @throws JMetalException if the parameter configuration is invalid or
   *                         the parameter type is unsupported
   */
  @SuppressWarnings("unchecked")
  private void processParameterDefinition(String parameterName, Map<String, Object> parameterConfig) {
    if (!parameterConfig.containsKey(TYPE_KEY)) {
      throw new JMetalException("Skipping parameter " + parameterName + " - missing type");
    }

    String parameterType = ((String) parameterConfig.get(TYPE_KEY)).toLowerCase();

    // For numeric types (integer, double), we allow either 'values' or 'range'
    // For categorical types, we require either 'values' or 'globalSubParameters'
    if (parameterType.equals(INTEGER_TYPE) || parameterType.equals(DOUBLE_TYPE) || parameterType.equals(REAL_TYPE)) {
      if (!parameterConfig.containsKey(VALUES_KEY) && !parameterConfig.containsKey(RANGE_KEY)) {
        throw new JMetalException("Skipping parameter " + parameterName + " - missing 'values' or 'range' for " + parameterType + " type");
      }
    } else if (!parameterConfig.containsKey(VALUES_KEY) && !parameterConfig.containsKey(GLOBAL_SUB_PARAMETERS_KEY)) {
      throw new JMetalException("Skipping parameter " + parameterName + " - missing 'values' or 'globalSubParameters' for " + parameterType + " type");
    }

    ParameterProcessor processor = parameterProcessors.get(parameterType);
    if (processor != null) {
      // Pass the entire parameter config map instead of just the values
      // The processor will extract what it needs
      processor.process(parameterName, parameterConfig, this);
    } else {
      throw new JMetalException("Unsupported parameter type: " + parameterType);
    }
  }
}