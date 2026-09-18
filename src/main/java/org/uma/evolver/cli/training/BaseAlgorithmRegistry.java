package org.uma.evolver.cli.training;

import java.util.Map;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Builds the base-level algorithm to be tuned, from a name and its parameter space.
 *
 * <p>Base-level algorithms are not built uniformly: {@code DoubleMOEAD} needs an extra
 * constructor argument ({@code weightVectorFilesDirectory}) that {@code DoubleNSGAII} does not.
 * {@link TrainingRequest#baseLevelExtraConfig()} carries that kind of algorithm-specific extra
 * configuration as a small string map instead of growing new dedicated request fields per
 * algorithm.
 *
 * <p>Prototype scope: only the two algorithms needed to reproduce the reference examples
 * ({@code NSGAIIOptimizingNSGAIIForProblemZDT4}, {@code NSGAIIOptimizingNSGAIIForBenchmarkRE3D}
 * and {@code NSGAIIOptimizingMOEADForProblemZDT4}) are registered.
 */
final class BaseAlgorithmRegistry {

  private BaseAlgorithmRegistry() {}

  static BaseLevelAlgorithm<DoubleSolution> resolve(
      String algorithmName,
      int populationSize,
      ParameterSpace parameterSpace,
      Map<String, String> extraConfig) {
    return switch (algorithmName) {
      case "NSGA-II" -> new DoubleNSGAII(populationSize, parameterSpace);
      case "MOEAD" -> new DoubleMOEAD(
          populationSize, requireExtra(extraConfig, "weightVectorFilesDirectory"), parameterSpace);
      default -> throw new JMetalException(
          "Unknown base-level algorithm: " + algorithmName + ". Supported: NSGA-II, MOEAD");
    };
  }

  private static String requireExtra(Map<String, String> extraConfig, String key) {
    String value = extraConfig == null ? null : extraConfig.get(key);
    if (value == null) {
      throw new JMetalException("Missing required baseLevelExtraConfig entry: " + key);
    }
    return value;
  }
}
