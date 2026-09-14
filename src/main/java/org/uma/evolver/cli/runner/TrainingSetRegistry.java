package org.uma.evolver.cli.runner;

import java.util.Map;
import java.util.function.Supplier;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.trainingset.ZDTTrainingSet;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Maps named, multi-problem training sets (as used e.g. by
 * {@code NSGAIIOptimizingNSGAIIForBenchmarkRE3D}) to their {@link TrainingSet} descriptor.
 *
 * <p>Prototype scope: only the sets needed to reproduce the reference examples are registered;
 * extend this map to support more (e.g. DTLZ3D, WFG2D, RWA3D already exist under
 * {@code org.uma.evolver.trainingset}).
 */
final class TrainingSetRegistry {

  private static final Map<String, Supplier<TrainingSet<DoubleSolution>>> TRAINING_SETS =
      Map.of(
          "ZDT", ZDTTrainingSet::new,
          "RE3D", RE3DTrainingSet::new);

  private TrainingSetRegistry() {}

  static TrainingSet<DoubleSolution> resolve(String trainingSetName) {
    Supplier<TrainingSet<DoubleSolution>> supplier = TRAINING_SETS.get(trainingSetName);
    if (supplier == null) {
      throw new JMetalException(
          "Unknown training set: " + trainingSetName + ". Supported: " + TRAINING_SETS.keySet());
    }
    return supplier.get();
  }
}
