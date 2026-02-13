package org.uma.evolver.analysis.ablation;

import java.util.Map;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;

/**
 * Validates configuration maps against a parameter space.
 */
public final class ConfigurationValidator {

  /**
   * Validates a configuration against a parameter space.
   *
   * @param parameterSpace the parameter space definition
   * @param configuration the configuration map
   * @throws IllegalArgumentException if validation fails
   */
  public void validate(ParameterSpace parameterSpace, Map<String, String> configuration) {
    if (parameterSpace == null) {
      throw new IllegalArgumentException("Parameter space cannot be null");
    }
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }

    var args = ConfigurationParser.toArgs(configuration);
    ParameterSpace instance = parameterSpace.createInstance();
    for (Parameter<?> parameter : instance.topLevelParameters()) {
      parameter.parse(args);
    }
  }
}
