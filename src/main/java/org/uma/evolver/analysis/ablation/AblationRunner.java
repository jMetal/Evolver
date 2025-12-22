package org.uma.evolver.analysis.ablation;

import org.uma.evolver.parameter.ParameterSpace;
// Removed Algorithm import as we rely on Functional Interface

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AblationRunner {

  /**
   * Runs the algorithm with the given configuration.
   *
   * @param parameterSpace  The base parameter space (will be cloned).
   * @param config          The configuration map to apply.
   * @param algorithmRunner Function to configure, run, and return result from
   *                        parameters.
   * @return The result of the execution.
   */
  public static <R> R run(
      ParameterSpace parameterSpace,
      Map<String, String> config,
      Function<ParameterSpace, R> algorithmRunner) {

    ParameterSpace clonedSpace = parameterSpace.createInstance();

    // Convert map to args array
    List<String> argsList = new ArrayList<>();
    for (var entry : config.entrySet()) {
      argsList.add("--" + entry.getKey());
      argsList.add(entry.getValue());
    }
    String[] args = argsList.toArray(new String[0]);

    // Apply configuration to all Top-Level parameters.
    clonedSpace.topLevelParameters().forEach(p -> p.parse(args));

    return algorithmRunner.apply(clonedSpace);
  }
}
