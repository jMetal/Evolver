package org.uma.evolver.parameter.yaml;

import org.uma.evolver.parameter.ParameterSpace;

/**
 * Builder class for creating ParameterSpace instances from YAML configuration files.
 * This class provides a simple interface to load and build parameter spaces
 * defined in YAML format.
 */
public class YAMLParameterSpaceBuilder {
    
    /**
     * Builds a ParameterSpace from a YAML configuration file.
     *
     * @param yamlFilePath Path to the YAML file relative to the classpath
     * @return A configured ParameterSpace instance
     */
    public ParameterSpace buildFromYAML(String yamlFilePath) {
        return new YAMLParameterSpace(yamlFilePath);
    }
}
